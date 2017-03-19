# Wrapper
Wrapper 通过自动生成一些代码，让你更愉快地调用 Listener
## 引入
在你的 `build.gradle`：
```groovy
dependencies {
    annotationProcessor 'com.linroid.wrapper:compiler:0.0.1'
    compile 'com.linroid.wrapper:library:0.0.1'
}
```

## 可以使用的注解：

 - `@WrapperClass` 对单个接口 / 类进行处理，默认只会进行判空处理

	```java
	@WrapperClass
	public interface SomeListener {
		void onFoo(View view);
	}
	```

 - `@UiThread` 需要进行 `Handler#post` 处理，可以用在方法或者类 / 接口上，如果用在类 / 接口，会对所有方法进行处理
	
	```java
	
	// @UiThread // 会对所有方法生效
	@WrapperClass
	public interface SomeListener {
		@UiThread // 只对指定方法生效
		void onFoo(View view);
		
		boolean onUserLeave();
	}
	```

 - `@WrapperMultiple` 支持多个 Listener

	```java
	@WrapperClass
	@WrapperMultiple
	public interface SomeListener {
	    void onFoo(View view);
	}
	```

 - `@WrapperGenerator` 与`@WrapperClass` 不同，你可以创建一个空的 Class，将所有需要处理的接口 / 类添加进来（这样就可以处理你无法修改的一些 Listener 了，比如 Android SDK 中的）。

	```java
	@WrapperGenerator(
	        values = {
	                View.OnClickListener.class,
	                View.OnLongClickListener.class,
	                MenuItem.OnMenuItemClickListener.class,
	                View.OnScrollChangeListener.class
	        }
	)
	@UiThread
	@WrapperMultiple
	public class SomeGenerator {
	}
	```

## 调用
经过 Wrapper 的处理，会生成一个包名相同的 `XXXWrapper` 的类，如果添加了 `@ WrapperMultiple ` 注解，会额外生成一个 `XXXMultiWrapper` 类。

需要注意的是，如果处理的是一个接口，那么生成的 Wrapper 会实现这个接口；而如果处理的是一个类，那么生成的 Wrapper 不会继承这个类。

添加完注解在执行一次 build 后，Wrapper 就会生成好相应的 `XXXWrapper` 类，使用它们非常简单：

```java
SomeListenerWrapper wrapper = new SomeListenerWrapper(listener);
// SomeListener wrapper = new SomeListenerWrapper(handler, listener); // 自己指定一个 Handler
wrapper.setWrapper(listener); // 设置你实现的 listener
wrapper.onFoo(view); // 调用方法
	
SomeListenerMultiWrapper multiWrapper = new SomeListenerMultiWrapper();
// SomeListenerMultiWrapper multiWrapper = new SomeListenerMultiWrapper(handler); // 自己指定一个 Handler
multiWrapper.addWrapper(listener); // 添加 listener
multiWrapper.onFoo(view); // 调用方法
```

## License
Copyright 2017 linroid

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
