/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.update.attributes.MpUnreachNlriBuilder;

/**
 * Common interface for NLRI parser implementation.
 */
public interface NlriParser {
    /**
     * Parse MP UN_REACH NLRI from buffer.
     *
     * @param nlri Encoded reachable NLRI in ByteBuf.
     * @param builder MP REACH NLRI builder.
     * @param constraint Peer specific constraints.
     * @throws BGPParsingException exception.
     */
    void parseNlri(
            @Nonnull final ByteBuf nlri,
            @Nonnull final MpReachNlriBuilder builder,
            @Nullable final PeerSpecificParserConstraint constraint)
            throws BGPParsingException;
    /**
     * Parse MP UN_REACH NLRI from buffer.
     *
     * @param nlri Encoded unreachable NLRI in ByteBuf.
     * @param builder MP UNREACH NLRI builder.
     * @param constraint Peer specific constraints.
     * @throws BGPParsingException exception.
     */
    void parseNlri(
            @Nonnull final ByteBuf nlri,
            @Nonnull final MpUnreachNlriBuilder builder,
            @Nullable final PeerSpecificParserConstraint constraint)
            throws BGPParsingException;
}
