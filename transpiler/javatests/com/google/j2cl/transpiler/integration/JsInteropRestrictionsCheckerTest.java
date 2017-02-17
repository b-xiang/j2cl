/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.integration;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests for JsInteropRestrictionsChecker.
 */
public class JsInteropRestrictionsCheckerTest extends IntegrationTestCase {

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testCollidingAccidentalOverrideConcreteMethodFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "interface Foo {",
            "  void doIt(Foo foo);",
            "}",
            "@JsType",
            "interface Bar {",
            "  void doIt(Bar bar);",
            "}",
            "class ParentBuggy {",
            "  public void doIt(Foo foo) {}",
            "  public void doIt(Bar bar) {}",
            "}",
            "public class Buggy extends ParentBuggy implements Foo, Bar {",
            "}")
        .assertCompileFails(
            "'void Baz.doIt(Bar)' and "
                + "'void Baz.doIt(Foo)' cannot both use the same JavaScript name 'doIt'.");
  }

  public void testCollidingAccidentalOverrideAbstractMethodFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "interface Foo {",
            "  void doIt(Foo foo);",
            "}",
            "@JsType",
            "interface Bar {",
            "  void doIt(Bar bar);",
            "}",
            "abstract class Baz implements Foo, Bar {",
            "  public abstract void doIt(Foo foo);",
            "  public abstract void doIt(Bar bar);",
            "}",
            "public class Buggy {}  // Unrelated class")
        .assertCompileFails(
            "'void Baz.doIt(Bar)' and "
                + "'void Baz.doIt(Foo)' cannot both use the same JavaScript name 'doIt'.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testCollidingAccidentalOverrideHalfAndHalfFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "interface Foo {",
            "  void doIt(Foo foo);",
            "}",
            "@JsType",
            "interface Bar {",
            "   void doIt(Bar bar);",
            "}",
            "class ParentParent {",
            "  public void doIt(Bar x) {}",
            "}",
            "@JsType",
            "class Parent extends ParentParent {",
            "  public void doIt(Foo x) {}",
            "}",
            "public class Buggy extends Parent implements Bar {}")
        .assertCompileFails(
            "'void Baz.doIt(Bar)' and "
                + "'void Baz.doIt(Foo)' cannot both use the same JavaScript name 'doIt'.");
  }

  public void testOverrideNoNameSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsIgnore;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsType;",
            "class Parent {",
            "  @JsMethod(name = \"a\")",
            "  public void ma() {}",
            "  @JsMethod(name = \"b\")",
            "  public void mb() {}",
            "}",
            "@JsType",
            "class Child1 extends Parent {",
            "  public void ma() {}",
            "  public void mb() {}",
            "}",
            "class Child2 extends Parent {",
            "  @JsMethod",
            "  public void ma() {}",
            "  @JsMethod",
            "  public void mb() {}",
            "}",
            "class Child3 extends Parent {",
            "  public void ma() {}",
            "  public void mb() {}",
            "}",
            "@JsType",
            "class Child4 extends Parent {",
            "  @JsIgnore",
            "  public void ma() {}",
            "  @JsIgnore",
            "  public void mb() {}",
            "}",
            "public class Buggy extends Parent {",
            "  Child1 c1;",
            "  Child2 c2;",
            "  Child3 c3;",
            "  Child4 c4;",
            "}")
        .assertCompileSucceeds();
  }

  public void testCollidingFieldExportsFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsProperty;",
            "public class Buggy {",
            "  @JsProperty",
            "  public static final int show = 0;",
            "  @JsProperty(name = \"show\")",
            "  public static final int display = 0;",
            "}")
        .assertCompileFails(
            "'int Buggy.display' and 'int Buggy.show' cannot both use the same "
                + "JavaScript name 'show'.");
  }

  public void testJsPropertyNonGetterStyleSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty(name = \"x\") int x();",
            "  @JsProperty(name = \"x\") void x(int x);",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsPropertyGetterStyleSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "public abstract class Buggy {",
            "  @JsProperty static native int getStaticX();",
            "  @JsProperty static native void setStaticX(int x);",
            "  @JsProperty abstract int getX();",
            "  @JsProperty abstract void setX(int x);",
            "  @JsProperty abstract boolean isY();",
            "  @JsProperty abstract void setY(boolean y);",
            "}")
        .assertCompileSucceeds();
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testJsPropertyIncorrectGetterStyleFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "public interface Buggy {",
            "  @JsProperty int isX();",
            "  @JsProperty int getY(int x);",
            "  @JsProperty void getZ();",
            "  @JsProperty void setX(int x, int y);",
            "  @JsProperty void setY();",
            "  @JsProperty int setZ(int z);",
            "  @JsProperty static void setStatic(){}",
            "  @JsProperty void setW(int... z);",
            "}")
        .assertCompileFails(
            "Line 6: JsProperty 'int EntryPoint.Buggy.isX()' cannot have a non-boolean return.",
            "Line 7: JsProperty 'int EntryPoint.Buggy.getY(int)' should have a correct setter "
                + "or getter signature.",
            "Line 8: JsProperty 'void EntryPoint.Buggy.getZ()' should have a correct setter "
                + "or getter signature.",
            "Line 9: JsProperty 'void EntryPoint.Buggy.setX(int, int)' should have a correct "
                + "setter or getter signature.",
            "Line 10: JsProperty 'void EntryPoint.Buggy.setY()' should have a correct setter "
                + "or getter signature.",
            "Line 11: JsProperty 'int EntryPoint.Buggy.setZ(int)' should have a correct setter "
                + "or getter signature.",
            "Line 12: JsProperty 'void EntryPoint.Buggy.setStatic()' should have a correct setter "
                + "or getter signature.",
            "Line 13: JsProperty 'void EntryPoint.Buggy.setW(int[])' cannot have a vararg "
                + "parameter.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testJsPropertyNonGetterStyleFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty boolean hasX();",
            "  @JsProperty int x();",
            "  @JsProperty void x(int x);",
            "}")
        .assertCompileFails(
            "Line 7: JsProperty 'boolean EntryPoint.Buggy.hasX()' should either follow Java Bean "
                + "naming conventions or provide a name.",
            "Line 8: JsProperty 'int EntryPoint.Buggy.x()' should either follow Java Bean "
                + "naming conventions or provide a name.",
            "Line 9: JsProperty 'void EntryPoint.Buggy.x(int)' should either follow Java Bean "
                + "naming conventions or provide a name.");
  }

  public void testCollidingJsPropertiesTwoGettersFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty",
            "  boolean isX();",
            "  @JsProperty",
            "  boolean getX();",
            "}")
        .assertCompileFails(
            "'boolean Buggy.getX()' and 'boolean Buggy.isX()' "
                + "cannot both use the same JavaScript name 'x'.");
  }


  public void testCollidingNativeJsPropertiesSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType(isNative=true)",
            "public class Buggy {",
            "  @JsMethod",
            "  public native int now();",
            "  @JsProperty",
            "  public native Object getNow();",
            "  @JsMethod",
            "  public static native int other();",
            "  @JsProperty",
            "  public static native Object getOther();",
            "  @JsMethod",
            "  public static native int another();",
            "  @JsProperty",
            "  public static Object another;",
            "}")
        .assertCompileSucceeds();
  }

  public void testCollidingJsPropertiesTwoSettersFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "public interface Buggy {",
            "  @JsProperty",
            "  void setX(boolean x);",
            "  @JsProperty",
            "  void setX(int x);",
            "}")
        .assertCompileFails(
            "'void Buggy.setX(int)' and "
                + "'void Buggy.setX(boolean)' cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingJsMethodAndJsPropertyGetterFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "interface IBuggy {",
            "  @JsMethod",
            "  boolean x(boolean foo);",
            "  @JsProperty",
            "  int getX();",
            "}",
            "public class Buggy implements IBuggy {",
            "  public boolean x(boolean foo) {return false;}",
            "  public int getX() {return 0;}",
            "}")
        .assertCompileFails(
            "'int IBuggy.getX()' and 'boolean IBuggy.x(boolean)' "
                + "cannot both use the same JavaScript name 'x'.",
            "'int Buggy.getX()' and 'boolean Buggy.x(boolean)' "
                + "cannot both use the same JavaScript name 'x'.");
  }

  public void testCollidingJsMethodAndJsPropertySetterFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "interface IBuggy {",
            "  @JsMethod",
            "  boolean x(boolean foo);",
            "  @JsProperty",
            "  void setX(int a);",
            "}",
            "public class Buggy implements IBuggy {",
            "  public boolean x(boolean foo) {return false;}",
            "  public void setX(int a) {}",
            "}")
        .assertCompileFails(
            "'void IBuggy.setX(int)' and 'boolean IBuggy.x(boolean)' "
                + "cannot both use the same JavaScript name 'x'.",
            "'void Buggy.setX(int)' and 'boolean Buggy.x(boolean)' "
                + "cannot both use the same JavaScript name 'x'.");
  }

  // TODO(rluble): enable when static property definitions are implemented.
  public void disabled_testCollidingPropertyAccessorExportsFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsProperty;",
            "public class Buggy {",
            "  @JsProperty",
            "  public static void setDisplay(int x) {}",
            "  @JsProperty(name = \"display\")",
            "  public static void setDisplay2(int x) {}",
            "}")
        .assertCompileFails(
            "Line 8: 'void EntryPoint.Buggy.setDisplay2(int)' cannot be exported because the "
                + "global name 'test.EntryPoint.Buggy.display' is already taken "
                + "by 'void EntryPoint.Buggy.setDisplay(int)'.");
  }

  public void testCollidingMethodExportsFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "public class Buggy {",
            "  @JsMethod",
            "  public static void show() {}",
            "  @JsMethod(name = \"show\")",
            "  public static void display() {}",
            "}")
        .assertCompileFails(
            " 'void Buggy.display()' and 'void Buggy.show()' cannot both use the same "
                + "JavaScript name 'show'");
  }

  public void testCollidingMethodToPropertyAccessorExportsFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "public class Buggy {",
            "  @JsProperty",
            "  public static void setShow(int x) {}",
            "  @JsMethod",
            "  public static void show() {}",
            "}")
        .assertCompileFails(
            " 'void Buggy.show()' and 'void Buggy.setShow(int)' cannot both use the same "
                + "JavaScript name 'show'");
  }

  public void testCollidingMethodToFieldExportsFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "public class Buggy {",
            "  @JsMethod",
            "  public static void show() {}",
            "  @JsProperty",
            "  public static final int show = 0;",
            "}")
        .assertCompileFails(
            "'int Buggy.show' and 'void Buggy.show()' cannot both use the same "
                + "JavaScript name 'show'.");
  }

  public void testCollidingMethodToFieldJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  public void show() {}",
            "  public final int show = 0;",
            "}")
        .assertCompileFails(
            "'int Buggy.show' and 'void Buggy.show()' "
                + "cannot both use the same JavaScript name 'show'.");
  }

  public void testCollidingMethodToMethodJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  public void show(int x) {}",
            "  public void show() {}",
            "}")
        .assertCompileFails(
            "'void Buggy.show()' and 'void Buggy.show(int)' "
                + "cannot both use the same JavaScript name 'show'.");
  }

  public void testCollidingSubclassExportedFieldToFieldJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "class ParentBuggy {",
            "  public int foo = 55;",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  public int foo = 110;",
            "}")
        .assertCompileSucceeds();
  }

  public void testCollidingSubclassExportedFieldToMethodJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "class ParentBuggy {",
            "  public int foo = 55;",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  public void foo(int a) {}",
            "}")
        .assertCompileSucceeds();
  }

  public void testCollidingSubclassExportedMethodToMethodJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "class ParentBuggy {",
            "  public void foo() {}",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  public void foo(int a) {}",
            "}")
        .assertCompileSucceeds();
  }

  public void testCollidingSubclassFieldToExportedFieldJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "class ParentBuggy {",
            "  public int foo = 55;",
            "}",
            "@JsType",
            "public class Buggy extends ParentBuggy {",
            "  public int foo = 110;",
            "}")
        .assertCompileSucceeds();
  }

  public void testCollidingSubclassFieldToExportedMethodJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "class ParentBuggy {",
            "  public int foo = 55;",
            "}",
            "@JsType",
            "public class Buggy extends ParentBuggy {",
            "  public void foo(int a) {}",
            "}")
        .assertCompileSucceeds();
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testCollidingSubclassFieldToFieldJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "class ParentBuggy {",
            "  public int foo = 55;",
            "}",
            "@JsType",
            "public class Buggy extends ParentBuggy {",
            "  public int foo = 110;",
            "}")
        .assertCompileFails(
            "Line 10: 'int EntryPoint.Buggy.foo' and 'int EntryPoint.ParentBuggy.foo' cannot both "
                + "use the same JavaScript name 'foo'.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testCollidingSubclassFieldToMethodJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "class ParentBuggy {",
            "  public int foo = 55;",
            "}",
            "@JsType",
            "public class Buggy extends ParentBuggy {",
            "  public void foo(int a) {}",
            "}")
        .assertCompileFails(
            "Line 10: 'void EntryPoint.Buggy.foo(int)' and 'int EntryPoint.ParentBuggy.foo' cannot "
                + "both use the same JavaScript name 'foo'.");
  }

  public void testCollidingSubclassMethodToExportedMethodJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "class ParentBuggy {",
            "  public void foo() {}",
            "}",
            "@JsType",
            "public class Buggy extends ParentBuggy {",
            "  public void foo(int a) {}",
            "}")
        .assertCompileSucceeds();
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testCollidingSubclassMethodToMethodInterfaceJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "interface IBuggy1 {",
            "  void show();",
            "}",
            "@JsType",
            "interface IBuggy2 {",
            "  void show(boolean b);",
            "}",
            "public class Buggy implements IBuggy1 {",
            "  public void show() {}",
            "}",
            "class Buggy2 extends Buggy implements IBuggy2 {",
            "  public void show(boolean b) {}",
            "}")
        .assertCompileFails(
            "Line 16: 'void EntryPoint.Buggy2.show(boolean)' and 'void EntryPoint.Buggy.show()' "
                + "cannot both use the same JavaScript name 'show'.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testCollidingSubclassMethodToMethodJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "class ParentBuggy {",
            "  public void foo() {}",
            "}",
            "@JsType",
            "public class Buggy extends ParentBuggy {",
            "  public void foo(int a) {}",
            "}")
        .assertCompileFails(
            "Line 10: 'void EntryPoint.Buggy.foo(int)' and 'void EntryPoint.ParentBuggy.foo()' "
                + "cannot both use the same JavaScript name 'foo'.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testCollidingSubclassMethodToMethodTwoLayerInterfaceJsTypeFails()
      throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "interface IParentBuggy1 {",
            "  void show();",
            "}",
            "interface IBuggy1 extends IParentBuggy1 {",
            "}",
            "@JsType",
            "interface IParentBuggy2 {",
            "  void show(boolean b);",
            "}",
            "interface IBuggy2 extends IParentBuggy2 {",
            "}",
            "public class Buggy implements IBuggy1 {",
            "  public void show() {}",
            "}",
            "class Buggy2 extends Buggy implements IBuggy2 {",
            "  public void show(boolean b) {}",
            "}")
        .assertCompileFails(
            "Line 20: 'void EntryPoint.Buggy2.show(boolean)' and 'void EntryPoint.Buggy.show()' "
                + "cannot both use the same JavaScript name 'show'.");
  }

  public void testNonCollidingSyntheticBridgeMethodSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "interface Comparable<T> {",
            "  int compareTo(T other);",
            "}",
            "@JsType",
            "class Enum<E extends Enum<E>> implements Comparable<E> {",
            "  public int compareTo(E other) {return 0;}",
            "}",
            "public class Buggy {}")
        .assertCompileSucceeds();
  }

  public void testCollidingSyntheticBridgeMethodSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "interface Comparable<T> {",
            "  int compareTo(T other);",
            "}",
            "@JsType",
            "class Enum<E extends Enum<E>> implements Comparable<E> {",
            "  public int compareTo(E other) {return 0;}",
            "}",
            "public class Buggy {}")
        .assertCompileSucceeds();
  }

  public void testSpecializeReturnTypeInImplementorSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "interface I {",
            "  I m();",
            "}",
            "@JsType",
            "class Buggy implements I {",
            "  public Buggy m() { return null; } ",
            "}")
        .assertCompileSucceeds();
  }

  public void testSpecializeReturnTypeInSubclassSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "class S {",
            "  public S m() { return null; }",
            "}",
            "@JsType",
            "public class Buggy extends S {",
            "  public Buggy m() { return null; } ",
            "}")
        .assertCompileSucceeds();
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testCollidingTwoLayerSubclassFieldToFieldJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "class ParentParentBuggy {",
            "  public int foo = 55;",
            "}",
            "class ParentBuggy extends ParentParentBuggy {",
            "  public int foo = 55;",
            "}",
            "@JsType",
            "public class Buggy extends ParentBuggy {",
            "  public int foo = 110;",
            "}")
        .assertCompileFails(
            "Line 13: 'int EntryPoint.Buggy.foo' and 'int EntryPoint.ParentParentBuggy.foo' cannot "
                + "both use the same JavaScript name 'foo'.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testShadowedSuperclassJsMethodFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "class ParentBuggy {",
            "  @JsMethod private void foo() {}",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  @JsMethod private void foo() {}",
            "}")
        .assertCompileFails(
            "Line 8: 'void EntryPoint.Buggy.foo()' and 'void EntryPoint.ParentBuggy.foo()' cannot "
                + "both use the same JavaScript name 'foo'.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testRenamedSuperclassJsMethodFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "@JsType",
            "class ParentBuggy {",
            "  public void foo() {}",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  @JsMethod(name = \"bar\") public void foo() {}",
            "}")
        .assertCompileFails(
            "Line 10: 'void EntryPoint.Buggy.foo()' cannot be assigned a different "
                + "JavaScript name than the method it overrides.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testRenamedSuperInterfaceJsMethodFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "@JsType",
            "interface ParentBuggy {",
            "  void foo();",
            "}",
            "public interface Buggy extends ParentBuggy {",
            "  @JsMethod(name = \"bar\") void foo();",
            "}")
        .assertCompileFails(
            "Line 10: 'void EntryPoint.Buggy.foo()' cannot be assigned a different "
                + "JavaScript name than the method it overrides.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testAccidentallyRenamedSuperInterfaceJsMethodFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "@JsType",
            "interface IBuggy {",
            "  void foo();",
            "}",
            "@JsType",
            "class ParentBuggy {",
            "  @JsMethod(name = \"bar\") public void foo() {}",
            "}",
            "public class Buggy extends ParentBuggy implements IBuggy {",
            "}")
        .assertCompileFails(
            "Line 11: 'void EntryPoint.ParentBuggy.foo()' "
                + "(exposed by 'EntryPoint.Buggy') "
                + "cannot be assigned a different JavaScript name than the method it overrides.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testRenamedSuperclassJsPropertyFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsProperty;",
            "class ParentBuggy {",
            "  @JsProperty public int getFoo() { return 0; }",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  @JsProperty(name = \"bar\") public int getFoo() { return 0; }",
            "}")
        .assertCompileFails(
            "Line 8: 'int EntryPoint.Buggy.getFoo()' "
                + "cannot be assigned a different JavaScript name than the method it overrides.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testJsPropertyDifferentFlavourInSubclassFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsProperty;",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "class ParentBuggy {",
            "  @JsProperty public boolean isFoo() { return false; }",
            "}",
            "public class Buggy extends ParentBuggy {",
            "  @JsProperty public boolean getFoo() { return false;}",
            "}")
        .assertCompileFails(
            "Line 10: 'boolean EntryPoint.Buggy.getFoo()' and 'boolean EntryPoint.ParentBuggy"
                + ".isFoo()' cannot both use the same JavaScript name 'foo'.");
  }

  public void testConsistentPropertyTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "interface IBuggy {",
            "  @JsProperty",
            "  public int getFoo();",
            "  @JsProperty",
            "  public void setFoo(int value);",
            "}",
            "public class Buggy implements IBuggy {",
            "  public int getFoo() {return 0;}",
            "  public void setFoo(int value) {}",
            "}")
        .assertCompileSucceeds();
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testInconsistentGetSetPropertyTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "interface IBuggy {",
            "  @JsProperty",
            "  public int getFoo();",
            "  @JsProperty",
            "  public void setFoo(Integer value);",
            "}",
            "public class Buggy implements IBuggy {",
            "  public int getFoo() {return 0;}",
            "  public void setFoo(Integer value) {}",
            "}")
        .assertCompileFails(
            "Line 10: JsProperty setter 'void EntryPoint.IBuggy.setFoo(Integer)' and "
                + "getter 'int EntryPoint.IBuggy.getFoo()' cannot have inconsistent types.",
            "Line 14: JsProperty setter 'void EntryPoint.Buggy.setFoo(Integer)' and "
                + "getter 'int EntryPoint.Buggy.getFoo()' cannot have inconsistent types.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testInconsistentIsSetPropertyTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType",
            "interface IBuggy {",
            "  @JsProperty",
            "  public boolean isFoo();",
            "  @JsProperty",
            "  public void setFoo(Object value);",
            "}",
            "public class Buggy implements IBuggy {",
            "  public boolean isFoo() {return false;}",
            "  public void setFoo(Object value) {}",
            "}")
        .assertCompileFails(
            "Line 10: JsProperty setter 'void EntryPoint.IBuggy.setFoo(Object)' and "
                + "getter 'boolean EntryPoint.IBuggy.isFoo()' cannot have inconsistent types.",
            "Line 14: JsProperty setter 'void EntryPoint.Buggy.setFoo(Object)' and "
                + "getter 'boolean EntryPoint.Buggy.isFoo()' cannot have inconsistent types.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testJsPropertySuperCallFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType class Super {",
            "  @JsProperty public int getX() { return 5; }",
            "}",
            "@JsType public class Buggy extends Super {",
            "  public int m() { return super.getX(); }",
            "}")
        .assertCompileFails(
            "Line 9: Cannot call property accessor 'int EntryPoint.Super.getX()' via super.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testJsPropertyOnStaticMethodFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType public class Buggy {",
            "  @JsProperty public static int getX() { return 0; }",
            "}")
        .assertCompileFails(
            "Line 6: Static property accessor 'int EntryPoint.Buggy.getX()' can only be native.");
  }

  public void testJsPropertyCallSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType class Super {",
            "  @JsProperty public int getX() { return 5; }",
            "}",
            "@JsType public class Buggy extends Super {",
            "  public int m() { return getX(); }",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsPropertyAccidentalSuperCallSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType class Super {",
            "  @JsProperty public int getX() { return 5; }",
            "}",
            "@JsType interface Interface {",
            "  @JsProperty int getX();",
            "}",
            "@JsType public class Buggy extends Super implements Interface {",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsPropertyOverrideSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType class Super {",
            "  @JsProperty public void setX(int x) {  }",
            "  @JsProperty public int getX() { return 5; }",
            "}",
            "@JsType public class Buggy extends Super {",
            "  @JsProperty public void setX(int x) {  }",
            "}")
        .assertCompileSucceeds();
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testMixingJsMethodJsPropertyFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "class Super {",
            "  @JsMethod public int getY() { return 5; }",
            "  @JsProperty public void setZ(int z) {}",
            "}",
            "public class Buggy extends Super {",
            "  @JsProperty(name = \"getY\") public int getY() { return 6; }",
            "  @JsMethod(name = \"z\") public void setZ(int z) {}",
            "}")
        .assertCompileFails(
            "Line 10: 'int EntryPoint.Buggy.getY()' and 'int EntryPoint.Super.getY()' cannot "
                + "both use the same JavaScript name 'getY'.",
            "Line 11: 'void EntryPoint.Buggy.setZ(int)' and 'void EntryPoint.Super.setZ(int)' "
                + "cannot both use the same JavaScript name 'z'.");
  }

  // GWT enforces some restriction on JSNI JsMethods. In J2CL,  JSNI is just a comment and no test
  // should fail for JSNI reasons.
  public void testJsMethodJSNIVarargsSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "public class Buggy {",
            "  @JsMethod public native void m(int i, int... z) /*-{ return arguments[i]; }-*/;",
            // The next method fails in GWT but should not fail in J2CL.
            "  @JsMethod public native void n(int i, int... z) /*-{ return z[0];}-*/;",
            "}")
        .assertCompileSucceeds();
  }

  public void testMultiplePrivateConstructorsExportSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  private Buggy() {}",
            "  private Buggy(int a) {}",
            "}")
        .assertCompileSucceeds();
  }

  public void testMultiplePublicConstructorsAllDelegatesToJsConstructorSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsIgnore;",
            "@JsType",
            "public class Buggy {",
            "  public Buggy() {}",
            "  @JsIgnore",
            "  public Buggy(int a) {",
            "    this();",
            "  }",
            "}",
            "class SubBuggy extends Buggy {",
            "  public SubBuggy() { this(1);}",
            "  public SubBuggy(int a) { super();}",
            "}",
            "@JsType",
            "class JsSubBuggy extends Buggy {",
            "  @JsIgnore",
            "  public JsSubBuggy() { this(1);}",
            "  public JsSubBuggy(int a) { super();}",
            "}",
            "@JsType (isNative = true)",
            "class NativeBuggy {",
            "  public NativeBuggy() {}",
            "  public NativeBuggy(int a) {}",
            "}",
            "@JsType (isNative = true)",
            "class NativeSubNativeBuggy extends NativeBuggy{",
            "  public NativeSubNativeBuggy() { super(1); }",
            "  public NativeSubNativeBuggy(int a) { super();}",
            "}",
            "class SubNativeBuggy extends NativeBuggy {",
            "  public SubNativeBuggy() { this(1);}",
            "  public SubNativeBuggy(int a) { super();}",
            "}",
            "class SubSubNativeBuggy extends NativeBuggy {",
            "  public SubSubNativeBuggy() { super(1);}",
            "  public SubSubNativeBuggy(int a) { this(); }",
            "}",
            "class SubNativeBuggyImplicitConstructor extends NativeBuggy {",
            "}")
        .assertCompileSucceeds();
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testMultipleConstructorsNonJsSubtypeRestrictionFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsIgnore;",
            "import jsinterop.annotations.JsConstructor;",
            "@JsType",
            "class BuggyJsType {",
            "  public BuggyJsType() {}",
            "  @JsIgnore",
            "  public BuggyJsType(int a) { this(); }",
            "}",
            "public class Buggy extends BuggyJsType {",
            // Error: two non-delegation constructors"
            "  public Buggy() {}",
            "  public Buggy(int a) { super(a); }",
            "}",
            "class SubBuggyJsType extends BuggyJsType {",
            // Correct: one non-delegating constructor targeting super primary constructor
            "  public SubBuggyJsType() { this(1); }",
            "  public SubBuggyJsType(int a) { super(); }",
            "}",
            "class SubSubBuggyJsType extends SubBuggyJsType {",
            // Error: non-delegating constructor target the wrong super constructor.
            "  public SubSubBuggyJsType() { this(1);}",
            "  public SubSubBuggyJsType(int a) { super(); }",
            "}",
            "class JsConstructorSubBuggyJsType extends SubBuggyJsType {",
            // Error: non-delegating constructor target the wrong super constructor.
            "  public JsConstructorSubBuggyJsType() { super(1);}",
            "  @JsConstructor",
            "  public JsConstructorSubBuggyJsType(int a) { super(); }",
            "}",
            "class OtherSubBuggyJsType extends BuggyJsType {",
            // Error: JsConstructor not delegating to super primary constructor.
            "  public OtherSubBuggyJsType() { super();}",
            "  @JsConstructor",
            "  public OtherSubBuggyJsType(int a) { this(); }",
            "}",
            "class AnotherSubBuggyJsType extends BuggyJsType {",
            // Error: Multiple JsConstructors in JsConstructor subclass.
            "  @JsConstructor",
            "  public AnotherSubBuggyJsType() { super();}",
            "  @JsConstructor",
            "  public AnotherSubBuggyJsType(int a) { this(); }",
            "}")
        .assertCompileFails(
            "Line 12: Class 'EntryPoint.Buggy' should have only one constructor delegating"
                + " to the superclass since it is subclass of a a type with JsConstructor.",
            "Line 22: Constructor 'EntryPoint.SubSubBuggyJsType.EntryPoint$SubSubBuggyJsType(int)' "
                + "can only delegate to super constructor "
                + "'EntryPoint.SubBuggyJsType.EntryPoint$SubBuggyJsType(int)' since it is a "
                + "subclass of a type with JsConstructor.",
            "Line 24: Class 'EntryPoint.JsConstructorSubBuggyJsType' should have only one "
                + "constructor delegating to the superclass since it is subclass of a a type with "
                + "JsConstructor.",
            "Line 27: Constructor "
                + "'EntryPoint.JsConstructorSubBuggyJsType.EntryPoint$JsConstructorSubBuggyJsType"
                + "(int)'"
                + " can be a JsConstructor only if all constructors in the class are delegating "
                + "to it.",
            "Line 32: Constructor 'EntryPoint.OtherSubBuggyJsType.EntryPoint$OtherSubBuggyJsType"
                + "(int)' "
                + "can be a JsConstructor only if all constructors in the class are delegating to "
                + "it.",
            "Line 34: More than one JsConstructor exists for 'EntryPoint.AnotherSubBuggyJsType'.",
            "Line 38: 'EntryPoint.AnotherSubBuggyJsType.EntryPoint$AnotherSubBuggyJsType(int)' "
                + "cannot "
                + "be exported because the global name 'test.EntryPoint.AnotherSubBuggyJsType' is "
                + "already taken by "
                + "'EntryPoint.AnotherSubBuggyJsType.EntryPoint$AnotherSubBuggyJsType()'.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testMultipleConstructorsNotAllDelegatedToJsConstructorFails()
      throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  public Buggy() {}",
            "  private Buggy(int a) {",
            "    new Buggy();",
            "  }",
            "}")
        .assertCompileFails(
            "Line 6: Constructor 'EntryPoint.Buggy.EntryPoint$Buggy()' can be a JsConstructor only "
                + "if all constructors in the class are delegating to it.");
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testMultiplePublicConstructorsExportFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  public Buggy() {}",
            "  public Buggy(int a) {",
            "    this();",
            "  }",
            "}")
        .assertCompileFails(
            "Line 5: More than one JsConstructor exists for EntryPoint.Buggy.",
            "Line 7: 'EntryPoint.Buggy.EntryPoint$Buggy(int)' cannot be exported because the "
                + "global name 'test.EntryPoint.Buggy' is already taken by "
                + "'EntryPoint.Buggy.EntryPoint$Buggy()'.");
  }

  public void testNonCollidingAccidentalOverrideSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "interface Foo {",
            "  void doIt(Object foo);",
            "}",
            "class ParentParent {",
            "  public void doIt(String x) {}",
            "}",
            "@JsType",
            "class Parent extends ParentParent {",
            "  public void doIt(Object x) {}",
            "}",
            "public class Buggy extends Parent implements Foo {}")
        .assertCompileSucceeds();
  }

  public void testJsNameInvalidNamesFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsPackage;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType(name = \"a.b.c\") public class Buggy {",
            "   @JsMethod(name = \"34s\") public void m() {}",
            "   @JsProperty(name = \"s^\") public int  m;",
            "   @JsProperty(name = \"\") public int n;",
            "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.b\") static void o() {}",
            "   @JsProperty(namespace = JsPackage.GLOBAL, name = \"a.c\") static int q;",
            "}",
            "@JsType(namespace = JsPackage.GLOBAL, name = \"a.b.d\")",
            "class OtherBuggy {",
            "}",
            "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"*\")",
            "class BadGlobalStar {",
            "}",
            "@JsType(namespace = JsPackage.GLOBAL, name = \"?\") interface BadGlobalWildcard {",
            "}",
            "@JsType(isNative = true, namespace = \"a.b\", name = \"*\") interface BadStar {",
            "}")
        .assertCompileFails(
            "'Buggy' has invalid name 'a.b.c'.",
            "'void Buggy.m()' has invalid name '34s'.",
            "'int Buggy.m' has invalid name 's^'.",
            "'int Buggy.n' cannot have an empty name.",
            "'void Buggy.o()' has invalid name 'a.b'.",
            "'int Buggy.q' has invalid name 'a.c'.",
            "OtherBuggy' has invalid name 'a.b.d'.",
            "Only native interfaces in the global namespace can be named '*'.",
            "Only native interfaces in the global namespace can be named '?'.",
            "Only native interfaces in the global namespace can be named '*'.");
  }

  public void testJsNameInvalidNamespacesFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType(namespace = \"a.b.\") public class Buggy {",
            "   @JsMethod(namespace = \"34s\") public static void m() {}",
            "   @JsProperty(namespace = \"s^\") public static int  n;",
            "   @JsMethod(namespace = \"\") public static void o() {}",
            "   @JsProperty(namespace = \"\") public int p;",
            "   @JsMethod(namespace = \"a\") public void q() {}",
            "}",
            "@JsType(namespace = \"<window>\") class JsTypeOnWindow{",
            "   @JsProperty(namespace = \"<window>\") public static int r;",
            "   @JsMethod(namespace = \"<window>\") public static  void s() {}",
            "}")
        .assertCompileFails(
            "'Buggy' has invalid namespace 'a.b.'.",
            "'void Buggy.m()' has invalid namespace '34s'.",
            "'int Buggy.n' has invalid namespace 's^'.",
            "'void Buggy.o()' cannot have an empty namespace.",
            "Instance member 'int Buggy.p' cannot declare a namespace.",
            "Instance member 'void Buggy.q()' cannot declare a namespace.");
  }

  public void testJsNameGlobalNamespacesSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsPackage;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType(namespace = JsPackage.GLOBAL) public class Buggy {",
            "   @JsMethod(namespace = JsPackage.GLOBAL) public static void m() {}",
            "   @JsProperty(namespace = JsPackage.GLOBAL) public static int n;",
            "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.b\")",
            "   public static native void o();",
            "}",
            "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"a.c\")",
            "class NativeOnGlobalNamespace {",
            "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.d\") static native void o();",
            "   @JsMethod(namespace = JsPackage.GLOBAL, name = \"a.e\") static native void getP();",
            "   @JsProperty(namespace = JsPackage.GLOBAL, name = \"a.f\") public static int n;",
            "}",
            "@JsType(isNative = true, namespace = \"<window>\", name = \"a.g\")",
            "class NativeOnWindowNamespace {",
            "   @JsMethod(namespace = \"<window>\", name = \"a.h\") static native void q();",
            "   @JsMethod(namespace = \"<window>\", name = \"a.i\") static native void getR();",
            "   @JsProperty(namespace = \"<window>\", name = \"a.j\") public static int s;",
            "}",
            "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"*\")",
            "interface Star {",
            "}",
            "@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = \"?\")",
            "interface Wildcard {",
            "}")
        .assertCompileSucceeds();
  }

  public void testSingleJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  public static void show1() {}",
            "  public void show2() {}",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsFunctionSucceeds() throws Exception {
    compile(
            source(
                "Function",
                "import jsinterop.annotations.JsFunction;",
                "import jsinterop.annotations.JsOverlay;",
                "@JsFunction",
                "public interface Function {",
                "  int getFoo();",
                "  @JsOverlay",
                "  static String s = new String();",
                "  @JsOverlay",
                "  default void m() {}",
                "  @JsOverlay",
                "  static void n() {}",
                "}"),
            source(
                "Buggy",
                "public final class Buggy implements Function {",
                "  public int getFoo() { return 0; }",
                "  public final void blah() {}",
                "  public void blat() {}",
                "  private void bleh() {}",
                "  static void blet() {",
                "    new Function() {",
                "       public int getFoo() { return 0; }",
                "    }.getFoo();",
                "  }",
                "  String x = new String();",
                "  static int y;",
                "}"))
        .assertCompileSucceeds();
  }

  public void testJsFunctionFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "import jsinterop.annotations.JsFunction;",
            "@JsFunction",
            "interface Function {",
            "  int getFoo();",
            "}",
            "public final class Buggy implements Function {",
            "  @JsProperty",
            "  public int getFoo() { return 0; }",
            "  @JsMethod",
            "  private void bleh() {}",
            "  @JsProperty",
            "  public int prop = 0;",
            "  public String toString() { return \"\"; }",
            "  public boolean equals(Object o) { return false; }",
            "  public int hashCode() { return 0; }",
            "}",
            "@JsFunction",
            "interface InvalidFunction {",
            "  @JsProperty",
            "  int getFoo();",
            "  default void m() {}",
            "  int f = 0;",
            "  static void n() {}",
            "}",
            "class NonFinalJsFunction implements Function {",
            "  public int getFoo() { return 0; }",
            "}",
            "@JsType",
            "final class JsFunctionMarkedAsJsType implements Function {",
            "  public int getFoo() { return 0; }",
            "}",
            "@JsFunction",
            "interface JsFunctionExtendsInterface extends Cloneable {",
            "  void foo();",
            "}",
            "interface InterfaceExtendsJsFunction extends Function {}",
            "class BaseClass { { if (new Object() instanceof Buggy) {} }}",
            "final class JsFunctionExtendingBaseClass extends BaseClass implements Function {",
            "  public int getFoo() { return 0; }",
            "}",
            "final class JsFunctionMultipleInterfaces implements Function, Cloneable {",
            "  public int getFoo() { return 0; }",
            "}",
            "@JsFunction @JsType",
            "interface InvalidJsTypeJsFunction {",
            "  void n();",
            "}",
            "@JsFunction",
            "class InvalidJsFunctionClass {",
            "}")
        .assertCompileFails(
            "'InvalidJsTypeJsFunction' cannot be both a JsFunction and a JsType at the same time.",
            "JsFunction 'InvalidJsFunctionClass' has to be a functional interface.",
            " JsFunction implementation 'NonFinalJsFunction' must be final.",
            "'JsFunctionMarkedAsJsType' cannot be both a JsFunction implementation and "
                + "a JsType at the same time.",
            "JsFunction 'JsFunctionExtendsInterface' cannot extend other interfaces.",
            "'InterfaceExtendsJsFunction' cannot extend JsFunction 'Function'.",
            "JsFunction implementation 'JsFunctionExtendingBaseClass' cannot extend a class.",
            "JsFunction implementation 'JsFunctionMultipleInterfaces' cannot implement more than"
                + " one interface.",
            "Cannot do instanceof against JsFunction implementation 'Buggy'."
            // TODO(b/27597597): There should be the following errors also.
            // "Line 14: JsFunction implementation member 'int EntryPoint.Buggy.getFoo()' "
            //      + "cannot be JsMethod nor JsProperty.",
            //  "Line 16: JsFunction implementation member 'void EntryPoint.Buggy.bleh()' cannot "
            //      + "be JsMethod nor JsProperty.",
            //  "Line 18: JsFunction implementation member 'int EntryPoint.Buggy.prop' cannot "
            //      + "be JsMethod nor JsProperty.",
            //  "Line 19: JsFunction implementation 'EntryPoint.Buggy' cannot implement method "
            //      + "'String EntryPoint.Buggy.toString()'.",
            //  "Line 20: JsFunction implementation 'EntryPoint.Buggy' cannot implement method "
            //      + "'boolean EntryPoint.Buggy.equals(Object)'.",
            //  "Line 21: JsFunction implementation 'EntryPoint.Buggy' cannot implement method "
            //      + "'int EntryPoint.Buggy.hashCode()'.",
            //  "Line 26: JsFunction interface member 'int EntryPoint.InvalidFunction.getFoo()'"
            //      + " cannot be JsMethod nor JsProperty.",
            //  "Line 27: JsFunction interface 'EntryPoint.InvalidFunction' cannot declare"
            //      + " non-JsOverlay member 'void EntryPoint.InvalidFunction.m()'.",
            //  "Line 28: JsFunction interface 'EntryPoint.InvalidFunction' cannot declare"
            //      + " non-JsOverlay member 'int EntryPoint.InvalidFunction.f'.",
            //  "Line 29: JsFunction interface 'EntryPoint.InvalidFunction' cannot declare"
            //      + " non-JsOverlay member 'void EntryPoint.InvalidFunction.n()'.",
            );
  }

  public void testNativeJsTypeStaticInitializerSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  static {",
            "    int x = 1;",
            "  }",
            "}",
            "@JsType(isNative = true)",
            "class Buggy2 {",
            "  static {",
            "    Object.class.getName();",
            "  }",
            "}")
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeInstanceInitializerFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  {",
            "    Object.class.getName();",
            "  }",
            "}",
            "@JsType(isNative = true)",
            "class Buggy2 {",
            "  {",
            "    int x = 1;",
            "  }",
            "}")
        .assertCompileFails(
            "Native JsType 'Buggy' cannot have initializer.",
            "Native JsType 'Buggy2' cannot have initializer.");
  }

  public void testNativeJsTypeNonEmptyConstructorFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  public Buggy(int n) {",
            "    n++;",
            "  }",
            "}")
        .assertCompileFails(
            "Native JsType constructor 'Buggy.Buggy(int)' cannot have non-empty method body.");
  }

  public void testNativeJsTypeImplicitSuperSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {",
                "  public Buggy(int n) {}",
                "}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Super {",
                "  public Super() {}",
                "}"))
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeExplicitSuperSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {",
                "  public Buggy(int n) {",
                "    super(n);",
                "  }",
                "}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Super {",
                "  public Super(int x) {}",
                "}"))
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeExplicitSuperWithEffectSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {",
                "  public Buggy(int n) {",
                "    super(n++);",
                "  }",
                "}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Super {",
                "  public Super(int x) {}",
                "}"))
        .assertCompileSucceeds();
  }

  public void testJsTypeInterfaceInInstanceofFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true) interface IBuggy {}",
            "@JsType public class Buggy {",
            "  public Buggy() { if (new Object() instanceof IBuggy) {} }",
            "}")
        .assertCompileFails("Cannot do instanceof against native JsType interface 'IBuggy'.");
  }

  public void testNativeJsTypeEnumFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public enum Buggy {",
            "  A,",
            "  B",
            "}")
        .assertCompileFails("Enum 'Buggy' cannot be a native JsType.");
  }

  public void testInnerNativeJsTypeFails() throws Exception {
    compile(
            "EntryPoint",
            "import jsinterop.annotations.JsType;",
            "public class EntryPoint {",
            "  @JsType(isNative = true)",
            "  public class Buggy {}",
            "}")
        .assertCompileFails("Non static inner class 'Buggy' cannot be a native JsType.");
  }

  public void testInnerJsTypeSucceeds() throws Exception {
    compile(
            "EntryPoint",
            "import jsinterop.annotations.JsType;",
            "public class EntryPoint {",
            "  @JsType",
            "  public static class Buggy {}",
            "}")
        .assertCompileSucceeds();
  }

  public void testLocalJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "public class Buggy { void m() { @JsType class Local {} } }")
        .assertCompileFails("Local class '$1Local' cannot be a JsType.");
  }

  public void testNativeJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Super {}"))
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeImplementsNativeJsTypeSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy implements Super {}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Super {}"))
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeInterfaceImplementsNativeJsTypeSucceeds() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Buggy extends Super {}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Super {}"))
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeExtendsJsTypeFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType",
                "public class Super {}"))
        .assertCompileFails("Native JsType 'Buggy' can only extend native JsType classes.");
  }

  public void testNativeJsTypeImplementsJsTypeInterfaceFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy implements Interface {}"),
            source(
                "Interface",
                "import jsinterop.annotations.JsType;",
                "@JsType",
                "public interface Interface {}"))
        .assertCompileFails("Native JsType 'Buggy' can only implement native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceExtendsJsTypeInterfaceFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Buggy extends Interface {}"),
            source(
                "Interface",
                "import jsinterop.annotations.JsType;",
                "@JsType",
                "public interface Interface {}"))
        .assertCompileFails("Native JsType 'Buggy' can only extend native JsType interfaces.");
  }

  public void testNativeJsTypeImplementsNonJsTypeFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy implements Interface {}"),
            source("Interface", "public interface Interface {}"))
        .assertCompileFails("Native JsType 'Buggy' can only implement native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceExtendsNonJsTypeFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Buggy extends Super {}"),
            source("Super", "public interface Super {}"))
        .assertCompileFails("Native JsType 'Buggy' can only extend native JsType interfaces.");
  }

  public void testNativeJsTypeInterfaceDefaultMethodsFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsOverlay;",
            "@JsType(isNative=true) interface Interface {",
            "  @JsOverlay default void someOtherMethod(){}",
            "}",
            "class OtherClass implements Interface {",
            "  public void someOtherMethod() {}",
            "}",
            "@JsType(isNative=true) public interface Buggy extends Interface {",
            "  default void someMethod(){}",
            "  void someOtherMethod();",
            "}")
        .assertCompileFails(
            "Native JsType method 'void Buggy.someMethod()' should be native or abstract."

            // TODO(b/27597597): Finalize checker implementation and enable this test.
            //  "Line 9: Method 'void EntryPoint.OtherClass.someOtherMethod()' cannot override a "
            //      + "JsOverlay method 'void EntryPoint.Interface.someOtherMethod()'.",
            //  "Line 13: Method 'void EntryPoint.Buggy.someOtherMethod()' cannot override a"
            //      + " JsOverlay method 'void EntryPoint.Interface.someOtherMethod()'."
            );
  }

  public void testJsOptionalSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsConstructor;",
            "import jsinterop.annotations.JsFunction;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsOptional;",
            "public class Buggy<T> {",
            "  @JsConstructor public Buggy(@JsOptional Object a) {}",
            "  @JsMethod public void foo(int a, Object b, @JsOptional String c) {}",
            "  @JsMethod public void bar(int a, @JsOptional Object b, @JsOptional String c) {}",
            "  @JsMethod public void baz(@JsOptional String a, @JsOptional Object b) {}",
            "  @JsMethod public void qux(@JsOptional String c, Object... os) {}",
            "  @JsMethod public void corge(int a, @JsOptional T b, String... c) {}",
            "}",
            "class SubBuggy extends Buggy<String> {",
            "  public SubBuggy() { super(null); } ",
            "  @JsMethod public void bar(int a, @JsOptional String b, String... c) {}",
            "}",
            "@JsFunction interface Function {",
            "  void m(String a, @JsOptional String b);",
            "}",
            "final class FunctionImpl implements Function {",
            "   public void m(String a, @JsOptional String b) {}",
            "}")
        .assertCompileSucceeds();
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testJsOptionalNotJsOptionalOverrideFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsOptional;",
            "interface Interface {",
            "   @JsMethod void m(@JsOptional Object o);",
            "}",
            "public class Buggy implements Interface {",
            "   @JsMethod public void m(Object o) {}",
            "}")
        .assertCompileFails(
            "Line 9: Method 'void EntryPoint.Buggy.m(Object)' should declare "
                + "parameter 'o' as JsOptional");
  }

  public void testJsOptionalNotAtEndFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsConstructor;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsOptional;",
            "public class Buggy {",
            "   @JsConstructor",
            "   public Buggy(@JsOptional String a, Object b, @JsOptional String c) {}",
            "   @JsMethod",
            "   public void bar(int a, @JsOptional Object b, String c) {}",
            "   @JsMethod",
            "   public void baz(@JsOptional Object b, String c, Object... os) {}",
            "}")
        .assertCompileFails(
            "JsOptional parameter 'a' in method "
                + "'Buggy.Buggy(String, Object, String)' cannot precede parameters that are not "
                + "JsOptional.",
            "JsOptional parameter 'b' in method "
                + "'void Buggy.bar(int, Object, String)' cannot precede parameters that are not "
                + "JsOptional.",
            "JsOptional parameter 'b' in method "
                + "'void Buggy.baz(Object, String, Object[])' cannot precede parameters that are"
                + " not JsOptional.");
  }

  public void testJsOptionalOnInvalidParametersFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsConstructor;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsOptional;",
            "public class Buggy {",
            "   @JsConstructor public Buggy(@JsOptional int a) {}",
            "   @JsMethod public void bar(int a, @JsOptional Object b, @JsOptional String... c) {}",
            "}")
        .assertCompileFails(
            "JsOptional parameter 'a' in method '"
                + "Buggy.Buggy(int)' cannot be of a primitive type.",
            "JsOptional parameter 'c' in method "
                + "'void Buggy.bar(int, Object, String[])' cannot be a varargs parameter.");
  }

  public void testJsOptionalOnNonJsExposedMethodsFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsProperty;",
            "import jsinterop.annotations.JsOptional;",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsOverlay;",
            "public class Buggy {",
            "  public void fun(int a, @JsOptional Object b, @JsOptional String c) {}",
            "  @JsProperty public void bar(@JsOptional Object o) {}",
            "}",
            "@JsType(isNative = true) class Native {",
            "  @JsOverlay public final void fun( @JsOptional Object a) {}",
            "}")
        .assertCompileFails(
            "JsOptional parameter in 'void Buggy.fun(int, Object, "
                + "String)' can only be declared in a JsMethod, a JsConstructor or a JsFunction.",
            "JsOptional parameter in 'void Buggy.bar(Object)' can only "
                + "be declared in a JsMethod, a JsConstructor or a JsFunction.",
            "JsOptional parameter in 'void Native.fun(Object)' can only "
                + "be declared in a JsMethod, a JsConstructor or a JsFunction.");
  }

  public void testJsOverlayOnNativeJsTypeInterfaceSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public interface Buggy {",
            "  @JsOverlay Object obj = new Object();",
            "  @JsOverlay default void someOverlayMethod(){};",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsOverlayOnNativeJsTypeMemberSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true) final class FinalType {",
            "  @JsOverlay public void n() { }",
            "}",
            "@JsType(isNative=true) interface NativeInterface {",
            "  @JsOverlay public static Object object = new Object();",
            "  @JsOverlay public static final Object other = new Object();",
            "  @JsOverlay public Object another = new Object();",
            "  @JsOverlay public final Object yetAnother = new Object();",
            "}",
            "@JsType(isNative=true) public class Buggy {",
            "  @JsOverlay public static Object object = new Object();",
            "  @JsOverlay public static final Object other = new Object();",
            "  @JsOverlay public static void m() { }",
            "  @JsOverlay public static void m(int x) { }",
            "  @JsOverlay private static void m(boolean x) { }",
            "  @JsOverlay private void m(String x) { }",
            "  @JsOverlay public final void n() { }",
            "  @JsOverlay public final void n(int x) { }",
            "  @JsOverlay private final void n(boolean x) { }",
            "  @JsOverlay final void o() { }",
            "  @JsOverlay protected final void p() { }",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsOverlayImplementingInterfaceMethodFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsOverlay;",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy implements IBuggy {",
                "  @JsOverlay",
                "  public void m() {}",
                "}"),
            source(
                "IBuggy",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface IBuggy {",
                "  void m();",
                "}"))
        .assertCompileFails(
            "JsOverlay method 'void Buggy.m()' cannot override a supertype method.");
  }

  public void testJsOverlayOverridingSuperclassMethodFails() throws Exception {
    compile(
            source(
                "Buggy",
                "import jsinterop.annotations.JsOverlay;",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Buggy extends Super {",
                "  @JsOverlay",
                "  public void m() {}",
                "}"),
            source(
                "Super",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public class Super {",
                "  public native void m();",
                "}"))
        .assertCompileFails(
            "JsOverlay method 'void Buggy.m()' cannot override a supertype method.");
  }

  public void testJsOverlayOnNonFinalMethodAndInstanceFieldFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  @JsOverlay public final int f2 = 2;",
            "  @JsOverlay",
            "  public void m() {}",
            "  { int v = f2; }",
            "}")
        .assertCompileFails(
            "Native JsType 'Buggy' cannot have initializer.",
            "JsOverlay field 'int Buggy.f2' can only be static.",
            "JsOverlay method 'void Buggy.m()' cannot be non-final nor native.");
  }

  public void testJsOverlayWithStaticInitializerSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  @JsOverlay public static final Object f1 = new Object();",
            "  @JsOverlay public static int f2 = 2;",
            "  static { f2 = 3; }",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsOverlayOnNativeMethodFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  @JsOverlay",
            "  public static final native void m1();",
            "  @JsOverlay",
            "  public final native void m2();",
            "}")
        .assertCompileFails(
            "JsOverlay method 'void Buggy.m1()' cannot be non-final nor native.",
            "JsOverlay method 'void Buggy.m2()' cannot be non-final nor native.");
  }

  public void testJsOverlayOnJsMemberFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsConstructor;",
            "import jsinterop.annotations.JsProperty;",
            "@JsType(isNative=true) public class Buggy {",
            "  @JsOverlay public Buggy() { }",
            "  @JsMethod @JsOverlay public final void m() { }",
            "  @JsMethod @JsOverlay public static void n() { }",
            "  @JsProperty @JsOverlay public static void setA(String value) { }",
            "}")
        .assertCompileFails(
            "The annotation @JsOverlay is disallowed for this location"
            // TODO(b/27597597): Finalize checker implementation and enable this test.
            //
            //  "Line 10: JsOverlay method 'EntryPoint.Buggy.EntryPoint$Buggy(int)' cannot be a "
            //      + "constructor.",
            //  "Line 11: JsOverlay method 'void EntryPoint.Buggy.m()' cannot be nor override"
            //      + " a JsProperty or a JsMethod.",
            //  "Line 12: JsOverlay method 'void EntryPoint.Buggy.n()' cannot be nor override"
            //      + " a JsProperty or a JsMethod.",
            //  "Line 13: JsOverlay method 'void EntryPoint.Buggy.setA(String)' cannot be nor "
            //      + "override a JsProperty or a JsMethod."
            );
  }

  public void testJsOverlayOnNonNativeJsTypeFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsOverlay;",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  @JsOverlay public static final int F = 2;",
            "  @JsOverlay",
            "  public final void m() {};",
            "}")
        .assertCompileFails(
            "JsOverlay 'int Buggy.F' can only be declared in a native type "
                + "or @JsFunction interface.",
            "JsOverlay 'void Buggy.m()' can only be declared in a native type "
                + "or @JsFunction interface.");
  }

  public void testJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true) class Super {",
            "}",
            "@JsType public class Buggy extends Super {",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsTypeExtendsNonJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "class Super {",
            "}",
            "@JsType public class Buggy extends Super {",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsTypeImplementsNativeJsTypeInterfaceSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true) interface Interface {",
            "}",
            "@JsType public class Buggy implements Interface {",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsTypeImplementsNonJsTypeInterfaceSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "interface Interface {",
            "}",
            "@JsType public class Buggy implements Interface {",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsTypeInterfaceExtendsNativeJsTypeInterfaceSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true) interface Interface {",
            "}",
            "@JsType public interface Buggy extends Interface {",
            "}")
        .assertCompileSucceeds();
  }

  public void testJsTypeInterfaceExtendsNonJsTypeInterfaceSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "interface Interface {",
            "}",
            "@JsType public interface Buggy extends Interface {",
            "}")
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeExtendsNaiveJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "@JsType(isNative=true) class Super {",
            "  public native int hashCode();",
            "}",
            "@JsType(isNative=true) interface HasHashCode {",
            "  int hashCode();",
            "}",
            "@JsType(isNative=true) public class Buggy extends Super {",
            "  public native String toString();",
            "  public native boolean equals(Object obj);",
            "}",
            "@JsType(isNative=true) class OtherBuggy implements HasHashCode {",
            "  public native String toString();",
            "  public native boolean equals(Object obj);",
            "  public native int hashCode();",
            "}",
            "@JsType(isNative=true) class NativeType {}",
            "interface A { int hashCode(); }",
            "class SomeClass extends NativeType implements A {",
            "  public int hashCode() { return 0; }",
            "}",
            "@JsType(isNative=true) interface NativeInterface {}",
            "class B { @JsMethod(name=\"something\") public int hashCode() { return 0; } }",
            "class SomeClass3 extends B implements NativeInterface {}")
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeBadMembersFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsIgnore;",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "@JsType(isNative=true) interface Interface {",
            "  @JsIgnore public void n();",
            "}",
            "@JsType(isNative=true) class Buggy {",
            "  public static final int s = 42;",
            "  public static int t = 42;",
            "  public final int f = 42;",
            "  public int g = 42;",
            "  @JsIgnore public Buggy() { }",
            "  @JsIgnore public int x;",
            "  @JsIgnore public native void n();",
            "  public void o() {}",
            "  public native void p() /*-{}-*/;",
            "}",
            "@JsType(isNative=true) class NativeType {}",
            "interface A { @JsMethod(name=\"something\") int hashCode(); }",
            "class SomeClass extends NativeType implements A {",
            "  public int hashCode() { return 0; }",
            "}",
            "interface B { int hashCode(); }",
            "class SomeClass2 extends NativeType implements B {",
            "}",
            "@JsType(isNative=true) class NativeTypeWithHashCode {",
            "  public native int hashCode();",
            "}",
            "class SomeClass3 extends NativeTypeWithHashCode implements A {}")
        .assertCompileFails(
            "Native JsType member 'void Interface.n()' cannot have @JsIgnore.",
            "Native JsType field 'int Buggy.f' cannot have initializer.",
            "Native JsType member 'Buggy.Buggy()' cannot have @JsIgnore.",
            "Native JsType member 'int Buggy.x' cannot have @JsIgnore.",
            "Native JsType member 'void Buggy.n()' cannot have @JsIgnore.",
            "Native JsType method 'void Buggy.o()' should be native or abstract.",
            "Native JsType field 'int Buggy.s' cannot have initializer.",
            "Native JsType field 'int Buggy.t' cannot have initializer.",
            "Native JsType field 'int Buggy.g' cannot have initializer."

            // TODO(b/27597597): Finalize checker implementation and enable this test.
            //  "Line 9: Native JsType 'EntryPoint.Buggy' cannot have initializer.",
            //  "Line 23: 'int EntryPoint.SomeClass.hashCode()' cannot be assigned a different "
            //      + "JavaScript name than the method it overrides.",
            //  "Line 26: Native JsType subclass 'EntryPoint.SomeClass2' can not implement "
            //      + "interface 'EntryPoint.B' that declares method 'hashCode' inherited "
            //      + "from java.lang.Object.",
            //  "Line 29: 'int EntryPoint.NativeTypeWithHashCode.hashCode()' "
            //      + "(exposed by 'EntryPoint.SomeClass3') cannot be assigned a different "
            //      + "JavaScript name than the method it overrides."
            );
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testSubclassOfNativeJsTypeBadMembersFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsIgnore;",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "@JsType(isNative=true) class NativeType {",
            "  @JsMethod(name =\"string\")",
            "  public native String toString();",
            "}",
            "class Buggy extends NativeType {",
            "  public String toString() { return super.toString(); }",
            "  @JsMethod(name = \"blah\")",
            "  public int hashCode() { return super.hashCode(); }",
            "}",
            "class SubBuggy extends Buggy {",
            "  public boolean equals(Object obj) { return super.equals(obj); }",
            "}")
        .assertCompileFails(
            "Line 8: Method 'String EntryPoint.NativeType.toString()' cannot override a method "
                + "from 'java.lang.Object' and change its name.",
            "Line 11: Cannot use super to call 'EntryPoint.NativeType.toString'. 'java.lang.Object'"
                + " methods in native JsTypes cannot be called using super.",
            "Line 13: 'int EntryPoint.Buggy.hashCode()' cannot be assigned a different JavaScript "
                + "name than the method it overrides.",
            "Line 13: Cannot use super to call 'EntryPoint.NativeType.hashCode'. "
                + "'java.lang.Object' methods in native JsTypes cannot be called using super.",
            "Line 16: Cannot use super to call 'EntryPoint.NativeType.equals'. 'java.lang.Object' "
                + "methods in native JsTypes cannot be called using super.");
  }

  public void testNativeMethodOnJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "public class Buggy {",
            "  @JsMethod",
            "  public native void m();",
            "}")
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsMethod;",
            "@JsType(isNative=true) abstract class Buggy {",
            "  public static native void m();",
            "  protected static native void m(Object o);",
            "  private static native void m(String o);",
            "  public Buggy() { }",
            "  protected Buggy(Object o) { }",
            "  private Buggy(String o) { }",
            "  public native void n();",
            "  protected native void n(Object o);",
            "  private native void n(String o);",
            "  public abstract void o();",
            "  protected abstract void o(Object o);",
            "  abstract void o(String o);",
            "}",
            "@JsType(isNative=true) abstract class NativeClass {",
            "  public native String toString();",
            "  public abstract int hashCode();",
            "}",
            "class NativeSubclass extends NativeClass {",
            "  public String toString() { return null; }",
            "  @JsMethod",
            "  public boolean equals(Object obj) { return false; }",
            "  public int hashCode() { return 0; }",
            "}",
            "class SubNativeSubclass extends NativeSubclass {",
            "  public boolean equals(Object obj) { return super.equals(obj); }",
            "}")
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeFieldsSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {",
            "  public static int f1;",
            "  protected static int f2;",
            "  private static int f3;",
            "  public int f4;",
            "  protected int f5;",
            "  private int f6;",
            "}")
        .assertCompileSucceeds();
  }

  public void testNativeJsTypeDefaultConstructorSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative = true)",
            "public class Buggy {}",
            "")
        .assertCompileSucceeds();
  }

  public void testNonJsTypeExtendingNativeJsTypeWithInstanceMethodSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true) class Super {",
            "  public native void m(Object o);",
            "  public native void m(Object[] o);",
            "}",
            "@JsType public class Buggy extends Super {",
            "  public void n(Object o) { }",
            "}")
        .assertCompileSucceeds();
  }

  public void testClassesExtendingNativeJsTypeInterfaceWithOverlaySucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsOverlay;",
            "@JsType(isNative=true) interface Super {",
            "  @JsOverlay default void fun() {}",
            "}",
            "@JsType(isNative=true) abstract class Buggy implements Super {",
            "}",
            "class JavaSubclass implements Super {",
            "}")
        .assertCompileSucceeds();
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void disabled_testNonJsTypeExtendingNativeJsTypeWithInstanceMethodOverloadsFails()
      throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true) class Super {",
            "  public native void m(Object o);",
            "  public native void m(int o);",
            "}",
            "public class Buggy extends Super {",
            "  public void m(Object o) { }",
            "}")
        .assertCompileFails(
            "Line 9: 'void EntryPoint.Buggy.m(Object)' and 'void EntryPoint.Super.m(int)' "
                + "cannot both use the same JavaScript name 'm'.");
  }

  public void testNonJsTypeWithNativeStaticMethodOverloadsSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "public class Buggy {",
            "  @JsMethod public static native void m(Object o);",
            "  @JsMethod public static native void m(int o);",
            "}")
        .assertCompileSucceeds();
  }

  // TODO(b/27597597): This test is in GWT but I think we should be allowing this type of collision.
  public void disabled_testNonJsTypeWithNativeInstanceMethodOverloadsFails() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsMethod;",
            "public class Buggy {",
            "  @JsMethod public native void m(Object o);",
            "  @JsMethod public void m(int o) { }",
            "}")
        .assertCompileFails(
            "Line 6: 'void EntryPoint.Buggy.m(int)' and 'void EntryPoint.Buggy.m(Object)' "
                + "cannot both use the same JavaScript name 'm'.");
  }

  public void testNonJsTypeExtendsJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType class Super {",
            "}",
            "public class Buggy extends Super {",
            "}")
        .assertCompileSucceeds();
  }

  public void testNonJsTypeImplementsJsTypeInterfaceSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType interface Interface {",
            "}",
            "public class Buggy implements Interface {",
            "}")
        .assertCompileSucceeds();
  }

  public void testNonJsTypeInterfaceExtendsJsTypeInterfaceSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType interface Interface {",
            "}",
            "public interface Buggy extends Interface {",
            "}")
        .assertCompileSucceeds();
  }

  public void testNonJsTypeExtendsNativeJsTypeSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true) class Super {",
            "  public native void m();",
            "}",
            "public class Buggy extends Super {",
            "  public void m() { }",
            "}")
        .assertCompileSucceeds();
  }

  public void testNonJsTypeImplementsNativeJsTypeInterfaceSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType(isNative=true) interface Interface {",
            "}",
            "public class Buggy implements Interface {",
            "}")
        .assertCompileSucceeds();
  }

  public void testNonJsTypeInterfaceExtendsNativeJsTypeInterfaceSucceeds() throws Exception {
    compile(
            source("Buggy", "public interface Buggy extends Interface {}"),
            source(
                "Interface",
                "import jsinterop.annotations.JsType;",
                "@JsType(isNative = true)",
                "public interface Interface {}"))
        .assertCompileSucceeds();
  }

  public void testUnusableByJsSuppressionSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "class A {}",
            "@JsType @SuppressWarnings(\"unusable-by-js\")", // SuppressWarnings on the class.
            "class B {",
            "  public A field;",
            "  public A t0(A a, A b) { return null; }",
            "}",
            "@JsType",
            "public class Buggy {",
            "  @SuppressWarnings(\"unusable-by-js\")", // add SuppressWarnings to field.
            "  public A field;",
            "  @SuppressWarnings({\"unusable-by-js\", \"unused\"})", // test multiple warnings.
            "  public A t0(A a, A b) { return null; }", // add SuppressWarnings to the method.
            "  public void t1(",
            "    @SuppressWarnings(\"unusable-by-js\")A a,",
            "    @SuppressWarnings(\"unusable-by-js\")A b",
            "  ) {}", // add SuppressWarnings to parameters.
            "}")
        .assertCompileSucceeds();
  }

  public void testUsableByJsTypesSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsFunction;",
            "@JsType class A {}",
            "@JsType interface I {}",
            "@JsFunction interface FI {void foo();}",
            "@JsType public class Buggy {",
            "  public void f1(boolean a, int b, double c) {}", // primitive types work fine.
            "  public void f2(Boolean a, Double b, String c) {}", // unboxed types work fine.
            "  public void f3(A a) {}", // JsType works fine.
            "  public void f4(I a) {}", // JsType interface works fine.
            "  public void f5(FI a) {}", // JsFunction works fine.
            "  public void f7(Object a) {}", // Java Object works fine.
            "  public void f8(boolean[] a) {}", // array of primitive types work fine.
            "  public void f9(Boolean[] a, Double[] b, String[] c) {}", // array of unboxed types.
            "  public void f10(A[] a) {}", // array of JsType works fine.
            "  public void f11(FI[] a) {}", // array of JsFunction works fine.
            "  public void f13(Object[] a) {}", // Object[] works fine.
            "  public void f14(Object[][] a) {}", // Object[][] works fine.
            "}")
        .assertCompileSucceeds();
  }

  public void testUnusableByJsNotExportedMembersSucceeds() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "class A {}",
            "@JsType public class Buggy {",
            "  private A field;", // private field.
            "  private A f1(A a) { return null; }", // private method.
            "}")
        .assertCompileSucceeds();
  }

  // TODO(b/27597597): Finalize checker implementation and enable this test.
  public void testUnusableByJsWarns() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "import jsinterop.annotations.JsFunction;",
            "import jsinterop.annotations.JsMethod;",
            "import jsinterop.annotations.JsProperty;",
            "class A {}",
            "@JsType interface I {}",
            "class B implements I {}",
            "class C {", // non-jstype class with JsMethod
            "  @JsMethod",
            "  public static void fc1(A a) {}", // JsMethod
            "}",
            "class D {", // non-jstype class with JsProperty
            "  @JsProperty",
            "  public static A a;", // JsProperty
            "}",
            "@JsFunction interface FI  { void f(A a); }", // JsFunction method is checked.
            "@JsType public class Buggy {",
            "  public A f;", // exported field
            "  public A f1(A a) { return null; }", // regular class fails.
            "  public A[] f2(A[] a) { return null; }", // array of regular class fails.
            "  public long f3(long a) { return 1l; }", // long fails.
            // non-JsType class that implements a JsType interface fails.
            "  public B f4(B a) { return null; }",
            "}")
        .assertCompileSucceeds();

    // TODO(b/27597597): Finalize checker implementation and enable this test.
    //  "Line 12: [unusable-by-js] Type of parameter 'a' in "
    //    + "'void EntryPoint.C.fc1(EntryPoint.A)' is not usable by but exposed to JavaScript.",
    //  "Line 16: [unusable-by-js] Type of 'EntryPoint.A EntryPoint.D.a' is not usable by but "
    //      + "exposed to JavaScript.",
    //  "Line 18: [unusable-by-js] Type of parameter 'a' in 'void "
    //      + "EntryPoint.FI.f(EntryPoint.A)' is not usable by but exposed to JavaScript.",
    //  "Line 20: [unusable-by-js] Type of 'EntryPoint.A EntryPoint.Buggy.f' is not usable by "
    //      + "but exposed to JavaScript.",
    //  "Line 21: [unusable-by-js] Return type of 'EntryPoint.A EntryPoint.Buggy.f1(EntryPoint"
    //      + ".A)' is not usable by but exposed to JavaScript.",
    //  "Line 21: [unusable-by-js] Type of parameter 'a' in "
    //      + "'EntryPoint.A EntryPoint.Buggy.f1(EntryPoint.A)' is not usable by but "
    //      + "exposed to JavaScript.",
    //  "Line 22: [unusable-by-js] Return type of "
    //      + "'EntryPoint.A[] EntryPoint.Buggy.f2(EntryPoint.A[])' is not usable by but "
    //      + "exposed to JavaScript.",
    //  "Line 22: [unusable-by-js] Type of parameter 'a' in "
    //      + "'EntryPoint.A[] EntryPoint.Buggy.f2(EntryPoint.A[])' is not usable by but "
    //      + "exposed to JavaScript.",
    //  "Line 23: [unusable-by-js] Return type of 'long EntryPoint.Buggy.f3(long)' is not "
    //      + "usable by but exposed to JavaScript.",
    //  "Line 23: [unusable-by-js] Type of parameter 'a' in "
    //      + "'long EntryPoint.Buggy.f3(long)' is not usable by but exposed to JavaScript.",
    //  "Line 24: [unusable-by-js] Return type of 'EntryPoint.B EntryPoint.Buggy.f4(EntryPoint"
    //      + ".B)' is not usable by but exposed to JavaScript.",
    //  "Line 24: [unusable-by-js] Type of parameter 'a' in "
    //      + "'EntryPoint.B EntryPoint.Buggy.f4(EntryPoint.B)' is not usable by but "
    //      + "exposed to JavaScript.");
  }

  public void testUnusableByJsAccidentalOverrideSuppressionWarns() throws Exception {
    compile(
            "Buggy",
            "import jsinterop.annotations.JsType;",
            "@JsType",
            "interface Foo {",
            "  @SuppressWarnings(\"unusable-by-js\") ",
            "  void doIt(Class foo);",
            "}",
            "class Parent {",
            "  public void doIt(Class x) {}",
            "}",
            "public class Buggy extends Parent implements Foo {}")
        .assertCompileSucceeds();
    // TODO(b/27597597): Finalize checker implementation and enable this test.
    //  "Line 10: [unusable-by-js] Type of parameter 'x' in "
    //      + "'void EntryPoint.Parent.doIt(Class)' (exposed by 'EntryPoint.Buggy') is not "
    //      + "usable by but exposed to JavaScript.");
  }

  private TranspileResult compile(String mainClass, String... source) throws Exception {
    return compile(source(mainClass, source));
  }

  private TranspileResult compile(Source... sources) throws Exception {
    File tempDir = Files.createTempDirectory("interop_checker").toFile();

    File inputDir = new File(tempDir, "input");
    inputDir.mkdir();
    File outputDir = new File(tempDir, "output");
    outputDir.mkdir();
    File packageDir = new File(inputDir, "test");
    packageDir.mkdir();

    for (Source source : sources) {
      Files.write(
          new File(packageDir, source.mainClass + ".java").toPath(),
          source.content,
          Charset.forName("UTF-8"));
    }
    return transpile(getTranspilerArgs(inputDir, outputDir), outputDir);
  }

  private String[] getTranspilerArgs(File inputDir, File outputDir) {
    List<String> argList = new ArrayList<>();

    argList.add("-sourcepath");
    argList.add(inputDir.getAbsolutePath());

    // Output dir
    argList.add("-d");
    argList.add(outputDir.getAbsolutePath());

    // Input source
    List<File> sourceFiles = sourceFiles(inputDir);
    assertFalse(sourceFiles.isEmpty());
    for (File sourceFile : sourceFiles) {
      argList.add(sourceFile.getPath());
    }

    argList.addAll(Arrays.asList("-source", "1.8", "-encoding", "UTF-8", "-cp", JRE_PATH));

    return Iterables.toArray(argList, String.class);
  }

  private static List<File> sourceFiles(File directory) {
    try {
      return Files.walk(directory.toPath())
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".srcjar"))
          .map(Path::toFile)
          .collect(Collectors.toList());
    } catch (IOException e) {
      return null;
    }
  }

  private static class Source {
    String mainClass;
    List<String> content;

    Source(String mainClass, String... code) {
      this.mainClass = mainClass;
      this.content = Lists.newArrayList(code);
      this.content.add(0, "package test;");
    }
  }

  private static Source source(String mainClass, String... code) {
    return new Source(mainClass, code);
  }
}
