package com.plavajs.libs.example;

import com.plavajs.libs.simpleinject.annotation.SimpleComponent;
import com.plavajs.libs.simpleinject.annotation.SimpleInject;

@SimpleComponent
public class ThirdClass {

    private SecondClass secondClass;

    @SimpleInject
    private FourthClass fourthClass;

    public ThirdClass(SecondClass secondClass) {
        this.secondClass = secondClass;
    }

//    public ThirdClass(SecondClass secondClass, String someVar) {
//        this.secondClass = secondClass;
//        System.out.println(someVar);
//    }
}
