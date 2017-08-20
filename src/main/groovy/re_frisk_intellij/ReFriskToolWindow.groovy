package re_frisk_intellij;

import clojure.lang.Compiler
import clojure.lang.Symbol
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.sun.net.httpserver.HttpServer
import org.jetbrains.annotations.NotNull

import static com.intellij.openapi.util.Disposer.*
import com.github.shiraji.colormanager.view.*
import clojure.lang.RT;
import clojure.lang.Var;

import groovy.json.*

class ReFriskToolWindow implements ToolWindowFactory {
    @Override
    void createToolWindowContent(
            @NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            initClojure(project, toolWindow);
        }
        catch (Exception e) {
            println("WATAFAK")
            println(e.getMessage())
        }
    }

    static void initClojure(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        ClassLoader loader = ReFriskToolWindow.class.getClassLoader();

        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(loader);

            // dummy to force RT class load first, since it has a circular static
            // initializer loop with Compiler
            new RT();

            Compiler.LOADER.bindRoot(loader);

            try {
                RT.var("clojure.core", "require").invoke(Symbol.intern("re-frisk-intellij.core"));
                Var res = RT.var("re-frisk-intellij.core", "create-tool-window")
                        .invoke(project, toolWindow);
            } catch (Exception e) {
                println("Error1");
                println(e.getMessage());
                e.printStackTrace()
            }

        } catch (Exception e) {
            println("Error2");
            println(e.getMessage());
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }
}
