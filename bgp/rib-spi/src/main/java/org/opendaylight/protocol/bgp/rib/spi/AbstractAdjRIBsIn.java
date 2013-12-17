/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

@ThreadSafe
public abstract class AbstractAdjRIBsIn<I, D extends DataObject> implements AdjRIBsIn {
	protected abstract static class RIBEntryData<I, D extends DataObject> {
		private final PathAttributes attributes;

		protected RIBEntryData(final PathAttributes attributes) {
			this.attributes = Preconditions.checkNotNull(attributes);
		}

		public PathAttributes getPathAttributes() {
			return this.attributes;
		}

		protected abstract D getDataObject(I key);
	}

	/**
	 * A single RIB table entry, which holds multiple versions of the entry's state and elects the authoritative based
	 * on ordering specified by the supplied comparator.
	 * 
	 */
	private final class RIBEntry {
		/*
		 * TODO: we could dramatically optimize performance by using the comparator
		 *       to retain the candidate states ordered -- thus selection would occur
		 *       automatically through insertion, without the need of a second walk.
		 */
		private final Map<Peer, RIBEntryData<I, D>> candidates = new HashMap<>();
		private final I key;

		@GuardedBy("this")
		private InstanceIdentifier<?> name;
		@GuardedBy("this")
		private RIBEntryData<I, D> currentState;

		RIBEntry(final I key) {
			this.key = Preconditions.checkNotNull(key);
		}

		private InstanceIdentifier<?> getName() {
			if (this.name == null) {
				this.name = identifierForKey(AbstractAdjRIBsIn.this.basePath, this.key);
			}
			return this.name;
		}

		private RIBEntryData<I, D> findCandidate(final RIBEntryData<I, D> initial) {
			RIBEntryData<I, D> newState = initial;
			for (final RIBEntryData<I, D> s : this.candidates.values()) {
				if (newState == null || AbstractAdjRIBsIn.this.comparator.compare(newState.attributes, s.attributes) > 0) {
					newState = s;
				}
			}

			return newState;
		}

		private void electCandidate(final DataModificationTransaction transaction, final RIBEntryData<I, D> candidate) {
			LOG.trace("Electing state {} to supersede {}", candidate, this.currentState);

			if (this.currentState == null || !this.currentState.equals(candidate)) {
				transaction.putOperationalData(getName(), candidate.getDataObject(this.key));
				this.currentState = candidate;
			}
		}

		synchronized boolean removeState(final DataModificationTransaction transaction, final Peer peer) {
			final RIBEntryData<I, D> data = this.candidates.remove(peer);
			LOG.trace("Removed data {}", data);

			final RIBEntryData<I, D> candidate = findCandidate(null);
			if (candidate != null) {
				electCandidate(transaction, candidate);
			} else {
				LOG.trace("Final candidate disappeared, removing entry {}", this.name);
				transaction.removeOperationalData(this.name);
			}

			return this.candidates.isEmpty();
		}

		synchronized void setState(final DataModificationTransaction transaction, final Peer peer, final RIBEntryData<I, D> state) {
			this.candidates.put(Preconditions.checkNotNull(peer), Preconditions.checkNotNull(state));
			electCandidate(transaction, findCandidate(state));
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(AbstractAdjRIBsIn.class);
	private final Comparator<PathAttributes> comparator;
	private final InstanceIdentifier<Tables> basePath;
	private final Update eor;

	@GuardedBy("this")
	private final Map<I, RIBEntry> entries = new HashMap<>();

	@GuardedBy("this")
	private final Map<Peer, Boolean> peers = new HashMap<>();

	protected AbstractAdjRIBsIn(final DataModificationTransaction trans, final RibReference rib,
			final Comparator<PathAttributes> comparator, final TablesKey key) {
		this.comparator = Preconditions.checkNotNull(comparator);
		this.basePath = InstanceIdentifier.builder(rib.getInstanceIdentifier()).child(LocRib.class).child(Tables.class, key).toInstance();

		this.eor = new UpdateBuilder().setPathAttributes(
				new PathAttributesBuilder().addAugmentation(
						PathAttributes1.class,
						new PathAttributes1Builder().setMpReachNlri(
								new MpReachNlriBuilder().setAfi(key.getAfi()).setSafi(key.getSafi()).build()).build()).build()).build();

		trans.putOperationalData(this.basePath,
				new TablesBuilder().setAfi(key.getAfi()).setSafi(key.getSafi()).setUptodate(Boolean.FALSE).build());
	}

	private void setUptodate(final DataModificationTransaction trans, final Boolean uptodate) {
		final Tables t = (Tables) trans.readOperationalData(this.basePath);
		Preconditions.checkState(t != null);
		if (!uptodate.equals(t.isUptodate())) {
			LOG.debug("Table {} switching uptodate to {}", t, uptodate);
			trans.putOperationalData(this.basePath, new TablesBuilder(t).setUptodate(uptodate).build());
		}
	}

	@Override
	public synchronized void clear(final DataModificationTransaction trans, final Peer peer) {
		final Iterator<Map.Entry<I, RIBEntry>> i = this.entries.entrySet().iterator();
		while (i.hasNext()) {
			final Map.Entry<I, RIBEntry> e = i.next();

			if (e.getValue().removeState(trans, peer)) {
				i.remove();
			}
		}

		this.peers.remove(peer);
		setUptodate(trans, !this.peers.values().contains(Boolean.FALSE));
	}

	protected abstract InstanceIdentifier<?> identifierForKey(final InstanceIdentifier<Tables> basePath, final I id);

	protected synchronized void add(final DataModificationTransaction trans, final Peer peer, final I id, final RIBEntryData<I, D> data) {
		RIBEntry e = this.entries.get(Preconditions.checkNotNull(id));
		if (e == null) {
			e = new RIBEntry(id);
			this.entries.put(id, e);
		}

		e.setState(trans, peer, data);
		if (!this.peers.containsKey(peer)) {
			this.peers.put(peer, Boolean.FALSE);
			setUptodate(trans, Boolean.FALSE);
		}
	}

	protected synchronized void remove(final DataModificationTransaction trans, final Peer peer, final I id) {
		final RIBEntry e = this.entries.get(id);
		if (e != null && e.removeState(trans, peer)) {
			LOG.debug("Removed last state, removing entry for {}", id);
			this.entries.remove(id);
		}
	}

	@Override
	public final void markUptodate(final DataModificationTransaction trans, final Peer peer) {
		this.peers.put(peer, Boolean.TRUE);
		setUptodate(trans, !this.peers.values().contains(Boolean.FALSE));
	}

	@Override
	public final Update endOfRib() {
		return this.eor;
	}
}
