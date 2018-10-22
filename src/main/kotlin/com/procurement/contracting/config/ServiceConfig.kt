package com.procurement.contracting.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = ["com.procurement.contracting.service"])
class ServiceConfig