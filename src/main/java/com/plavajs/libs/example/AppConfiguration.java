package com.plavajs.libs.example;

import com.plavajs.libs.simpleinject.annotation.SimpleBean;
import com.plavajs.libs.simpleinject.annotation.SimpleConfiguration;

@SimpleConfiguration
public class AppConfiguration {

    @SimpleBean
    public String someVar() {
        return "abrakadabra";
    }

    @SimpleBean
    public FourthClass fourthClass() {
        return new FourthClass();
    }

    @SimpleBean
    public FourthClass fourthClassAAAA() {
        return new FourthClass();
    }
}
