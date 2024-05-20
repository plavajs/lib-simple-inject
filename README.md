# Simple inject
Library provides dependency injection container with simple tools (annotations) to manage them in your project without the need for using Spring or other extensive framework.
## Annotations
There are following annotations serving to manage your dependencies:
### `@SimpleComponent`
* identifies a class as a component to create a bean for it
* has 1 argument:
  * `identifier` ( String )
    * unique identifier of the bean created for the specific type
    * optional
    * default = `""`
* see also [`@SimpleBean`](#simplebean) 
* examples:
  1) without an identifier or with a blank value of the identifier
     * `identifier = ""`
     ```java
     @SimpleComponent
     // or @SimpleComponent(identifier = "")
     public class MyComponentClass {
     }
     ```
  2) with not blank identifier value
     ```java
     @SimpleComponent(identifier = "my-identifier")
     public class MyComponentClass {
     }
     ```
### `@SimpleBean`
* has 1 argument:
  * `identifier` ( String )
    * unique identifier of the bean created for the specific type
    * optional
    * default = `""`
* has 2 usages:
  * identifies a method as a bean in a `SimpleConfiguration` class (see [`@SimpleConfiguration`](#simpleconfiguration))
    * the class with `SimpleBean` methods must be `@SimpleConfiguration` annotated
      * if it is not -> the beans declared in it are not loaded
    * the `@SimpleBean` annotated method must be ```public static```
      * if it is not -> an exception is thrown
  * identifies a constructor in a `SimpleComponent` class as the one constructor which is used to create the `SimpleComponent` bean
    * if there is only one constructor in the `SimpleComponent` class -> the `@SimpleBean` annotation is optional
    * if there are multiple constructors in the `SimpleComponent` class -> one (and only one) must be `@SimpleBean` annotated
      * if none is `@SimpleBean` annotated -> an exception is thrown
      * if multiple are `@SimpleBean` annotated -> an exception is thrown
* examples:
  1) without an identifier or with a blank identifier value
     * `identifier = ""`
     ```java
     @SimpleBean
     // or @SimpleBean(identifier = "")
     public static MyClass myBean() {
        return new MyClass();
     }
     ```
  2) with not blank identifier value
     ```java
     @SimpleBean(identifier = "my_bean identifier")
     public static MyClass myBean() {
         return new MyClass();
     }
     ```
### `@SimpleConfiguration`
* identifies a class as a configuration class containing `@SimpleBean` annotated methods to be loaded as beans
* there may be multiple `SimpleConfiguration` classes in your project / module
* example:
    ```java
    @SimpleConfiguration
    public class MyConfigurationClass {
    
        @SimpleBean
        public static MyClass myBean() {
            return new MyClass();
        }
        
        @SimpleBean(identifier = "my_bean identifier")
        public static MyClass myBean() {
            return new MyClass();
        }
    }
    ```
### `@SimpleComponentScan`
* configures which packages are scanned for `@SimpleComponent` annotated classes
* it is repeatable (might be used multiple times on one class with various arguments)
  * but may be used on **one and only one** class (e.g. on `SimpleConfiguration` class - good practice but not a strict rule)
* has 2 arguments:
  * `packages` ( String[ ] )
    * package paths you want to scan for `SimpleComponent` classes
    * might be:
      * a String in case of only one package
      * or an array of Strings if you want more packages to be scanned
    * optional
    * default = `""`
  * `recursively` ( boolean )
    * says if the package path(s) in the first argument should be scanned recursively or not
    * optional
    * default = `true`
  * if `packages` argument is missing or has a blank value, the root packages of your project module are added to scan
    * example - when `packages` argument is empty:
        ```
        Project structure:
        my-project
            src
                main
                    java
                        com.example
                          myproject
                            MyFirstClass
                            MySecondClass
                          MyThirdClass
                          MyFourthClass
                        org.example.other
                            MyOtherClass
        
        => the root packages are: 'com' and 'org'
        The rest depends if the 'recursively' argument is true or false
      ```
* examples:
  1) empty or a blank `packages` value + empty `recursively` value
     * `packages = ""`
     * `recursively = true`
     * => the root packages are scanned recursively (in other words: the whole project is scanned)
     ```java
     @SimpleComponentScan
     // or @SimpleComponentScan(packages = "")
     // or @SimpleComponentScan(packages = "", recursively = true)
     public class MyApplication {
        public static void main(String[] args) {
        }
     }
     ```
  2) empty or a blank `packages` value + `recursively = false`
     * `packages = ""`
     * `recursively = false`
     * => only root packages are scanned (`com` and `org` in the example above) !!!NOT recursively!!! => not recommended
     ```java
     @SimpleComponentScan(recursively = false)
     // or @SimpleComponentScan(packages = "", recursively = false)
     public class MyApplication {
        public static void main(String[] args) {
        }
     }
     ```
  3) a blank in the `packages` values array
     * the root packages are **added** to be scanned
     ```java
     @SimpleComponentScan(packages = {"com.example", ""}, recursively = false)
     public class MyApplication {
        public static void main(String[] args) {
        }
     }
     ```
     * => the `com.example` is scanned and also the root packages are scanned (`com` and `org` in the example above) (all NOT recursively because of the value of the `recursively` argument)
  4) single `packages` value specified
     ```java
     @SimpleComponentScan(packages = "com.example", recursively = false)
     public class MyApplication {
         public static void main(String[] args) {
         }
     }
     ```
     * => only the `com.example` package is scanned (NOT recursively)
  5) multiple `packages` values specified
     ```java
     @SimpleComponentScan(packages = {"com.example", "com.example.myproject"}, recursively = false)
     public class MyApplication {
        public static void main(String[] args) {
        }
     }
     ```
  6) various combinations of the above
     ```java
     @SimpleComponentScan(packages = {"com.example", "org.example.other"}, recursively = false)
     @SimpleComponentScan(packages = "com") // => recursively = true
     public class MyApplication {
        public static void main(String[] args) {
        }
     }
     ```
     * following happens:
       * the `com.example` and `com.example.other` packages are marked for scanning NOT recursively
       * and the `com.example` package is marked for scanning recursively
       * which would be quite inefficient
         * and that is the reason there is a logic to exclude the first NOT recursive `com.example` package
           * only the recursive `com.example` (because it already contains the NOT recursive `com.example`) and NOT recursive `com.example.other` packages are scanned
### `@SimpleIdentifier`
* identifies the bean which should be used for instantiating the parameter
* has 2 usages:
  * in a `SimpleComponent` class' constructor parameters
  * in a `SimpleBean` method parameters
* when the `@SimpleIdentifier` is not specified, program searches for a bean **with a blank identifier**
* useful when there are multiple existing beans for the type of the specific parameter or a single bean with a NOT blank identifier
  * in case that there is **only one bean with a blank identifier** for the specific parameter, the `@SimpleIdentifier` is usable but **NOT mandatory**
  * in case that there is **only one bean with a NOT blank identifier** for the specific parameter, the `@SimpleIdentifier` is **mandatory**
* has 1 argument:
  * `value` ( String )
    * specifies the identifier value
    * mandatory
* examples:
  1) `SimpleComponent` class' constructor parameter usage
     ```java
     @SimpleComponent(identifier = "my-identifier")
     public class MyFirstClass {
     }
     ```
     ```java
     @SimpleComponent
     public class MySecondClass {
     
        private MyFirstClass myFirstClass;
        
        public MySecondClass(
          @SimpleBeanIdentifier("my-identifier")
          MyFirstClass myFirstClass) {
            this.myFirstClass = myFirstClass;
        }
     }
     ```
  2) `SimpleBean` method parameter usage
     ```java
     @SimpleComponent(identifier = "my-identifier")
     public class MyFirstClass {
     }
     ```
     ```java
     public class MySecondClass {   // may but has not be a component
     
        private MyFirstClass myFirstClass;
        
        public MySecondClass(MyFirstClass myFirstClass) {
            this.myFirstClass = myFirstClass;
        }
     }
     ```
     ```java
     @SimpleConfiguration
     public class MyConfigurationClass {
        
        @SimpleBean
        public static MySecondClass myBean(
          @SimpleBeanIdentifier("my-identifier")
          MyFirstClass myFirstClass) {
           
            return new MySecondClass(myFirstClass);
        }
     }
     ```
### `@SimpleInject`
* serves to inject fields in `SimpleComponents` without the need for being set up in a constructor
* has 1 argument:
  * `identifier`
    * serves to identify the bean which should be used for instantiating the field
    * optional
    * default = `""`
* example:
  1) with none or a blank identifier value
     ```java
     @SimpleComponent
     // or @SimpleComponent(identifier = "")
     public class MyFirstClass {
     }
     ```
     ```java
     public class MySecondClass {
     
        @SimpleInject
        // or @SimpleInject(identifier = "")
        private MyFirstClass myFirstClass;
     }
     ```
  2) with a NOT blank identifier value
     ```java
     @SimpleComponent(identifier = "my-identifier")
     public class MyFirstClass {
     }
     ```
     ```java
     @SimpleComponent
     public class MySecondClass {
     
        @SimpleInject(identifier = "my-identifier")
        private MyFirstClass myFirstClass;
     }
     ```
### `@SimpleEagerInstances`
* serves to decide when the beans are instantiated
* there are 2 possible scenarios:
  * the `@SimpleEagerInstances` is found anywhere in the module of your project:
    * there are all instances for **all** beans created at the first time any one of the beans is requested
  * the `@SimpleEagerInstances` is NOT found anywhere in the module of your project:
      * when an instance of a bean is requested
        * its 'injected' and 'constructor' fields are instantiated recursively
            * all beans for the fields are instantiated (if the beans exist, if not -> an exception is thrown)
            * and beans for their 'injected' and 'constructor' fields are instantiated
            * etc
      * there can be separate trees of such dependencies
        * in this case the other tree is instantiated at the moment when some instance for a bean from this tree is requested
          * if the requested bean is not the top bean only the bellow branches of the tree are instantiated at the moment