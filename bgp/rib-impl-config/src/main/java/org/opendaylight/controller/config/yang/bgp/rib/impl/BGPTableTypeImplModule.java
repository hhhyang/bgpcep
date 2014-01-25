/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Generated file

 * Generated from: yang module name: bgp-rib-impl  yang module local name: bgp-table-type-impl
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Sat Jan 25 20:28:03 CET 2014
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

/**
 *
 */
public final class BGPTableTypeImplModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPTableTypeImplModule
{

	public BGPTableTypeImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public BGPTableTypeImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
			final BGPTableTypeImplModule oldModule, final java.lang.AutoCloseable oldInstance) {

		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	protected void customValidation(){
		JmxAttributeValidationException.checkNotNull(getAfi(),
				"value is not set.", afiJmxAttribute);
		JmxAttributeValidationException.checkNotNull(getSafi(),
				"value is not set.", safiJmxAttribute);
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		return new AutoCloseableBgpTableType(getAfiIdentity(), getSafiIdentity());
	}

	private static final class AutoCloseableBgpTableType implements AutoCloseable, BgpTableType {
		private final Class<? extends AddressFamily> afi;
		private final Class<? extends SubsequentAddressFamily> safi;

		public AutoCloseableBgpTableType(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
			this.afi = Preconditions.checkNotNull(afi);
			this.safi = Preconditions.checkNotNull(safi);
		}

		@Override
		public Class<? extends DataContainer> getImplementedInterface() {
			return BgpTableType.class;
		}

		@Override
		public Class<? extends AddressFamily> getAfi() {
			return afi;
		}

		@Override
		public Class<? extends SubsequentAddressFamily> getSafi() {
			return safi;
		}

		@Override
		public void close() {
			// Nothing to do
		}
	}
}
