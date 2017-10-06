module tornadofx {
    requires java.logging;

    requires java.prefs;
    requires java.desktop;

    requires javafx.deploy;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires javafx.web;

    requires kotlin.stdlib;

    // Runtime optional dependencies - TODO: Look for Java 9 ready versions
    requires static org.apache.felix.framework;
    requires static httpcore;
    requires static httpclient;
    requires static javax.json;

    exports tornadofx;
    exports tornadofx.osgi;
    exports sun.net.www.protocol.css;
}