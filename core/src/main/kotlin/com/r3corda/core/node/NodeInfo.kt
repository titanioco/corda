package com.r3corda.core.node

import com.r3corda.core.crypto.Party
import com.r3corda.core.messaging.SingleMessageRecipient
import com.r3corda.core.node.services.ServiceInfo

/**
 * Info about a network node that acts on behalf of some form of contract party.
 */
data class NodeInfo(val address: SingleMessageRecipient, val identity: Party,
                    var advertisedServices: Set<ServiceInfo> = emptySet(),
                    val physicalLocation: PhysicalLocation? = null)
