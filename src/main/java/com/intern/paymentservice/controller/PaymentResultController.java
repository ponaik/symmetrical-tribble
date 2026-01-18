package com.intern.paymentservice.controller;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/totallyLegitDecisionApi")
public class PaymentResultController {

    @GetMapping
    public int generateRandomNumber() {
        return RandomUtils.secure().randomInt();
    }
}
