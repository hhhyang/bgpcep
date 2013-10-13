/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;

import com.google.common.base.Preconditions;

public final class SimpleAddressFamilyRegistry extends AbstractFamilyRegistry<AddressFamily, Integer> implements AddressFamilyRegistry {
	@Override
	public AutoCloseable registerAddressFamily(final Class<? extends AddressFamily> clazz, final int number) {
		Preconditions.checkArgument(number >= 0 && number <= 65535);
		return super.registerFamily(clazz, number);
	}

	@Override
	public Class<? extends AddressFamily> classForFamily(final int number) {
		return super.classForFamily(number);
	}

	@Override
	public Integer numberForClass(final Class<? extends AddressFamily> clazz) {
		return super.numberForClass(clazz);
	}
}
