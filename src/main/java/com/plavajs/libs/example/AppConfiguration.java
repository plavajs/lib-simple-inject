package com.plavajs.libs.example;

import com.plavajs.libs.simpleinject.annotation.SimpleBean;
import com.plavajs.libs.simpleinject.annotation.SimpleConfiguration;

@SimpleConfiguration
public class AppConfiguration {

    @SimpleBean
    public static String someVar() {
        return "abrakadabra";
    }

    @SimpleBean
    public static FourthClass fourthClass() {
        return new FourthClass();
    }

//    @SimpleBean
//    public FourthClass fourthClassAAAA() {
//        return new FourthClass();
//    }
}
