package com.reactivebbq.loyalty

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, property = "type")
trait SerializableMessage
