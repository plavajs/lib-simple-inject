package com.plavajs.libs.example;

import com.plavajs.libs.simpleinject.annotation.SimpleComponent;

@SimpleComponent
public class FirstClass {

    private SecondClass secondClass;
    private ThirdClass thirdClass;

    public FirstClass(SecondClass secondClass, ThirdClass thirdClass) {
        this.secondClass = secondClass;
        this.thirdClass = thirdClass;
    }
}
