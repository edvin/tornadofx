import tornadofx.ChildInterceptor;
import tornadofx.Stylesheet;

module tornadofx {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires javafx.media;

    requires kotlin.stdlib;
    requires kotlin.reflect;

    requires static org.apache.httpcomponents.httpcore;
    requires static org.apache.httpcomponents.httpclient;
    requires static org.apache.felix.framework;

    requires transitive java.json;
    requires transitive java.prefs;
    requires transitive java.logging;

    opens tornadofx to javafx.fxml;

    exports tornadofx;

    uses ChildInterceptor;
    uses Stylesheet;
}