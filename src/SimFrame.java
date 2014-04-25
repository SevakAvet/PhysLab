import javax.swing.*;
import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;

class SimWindowAdapter extends WindowAdapter {
    Applet applet = null;

    public SimWindowAdapter(Applet applet) {
        this.applet = applet;
    }

    public void windowIconified(WindowEvent e) {
        applet.stop();
    }

    public void windowDeiconified(WindowEvent e) {
        applet.start();
    }

    public void windowDeactivated(WindowEvent e) {
        applet.stop();
    }

    public void windowActivated(WindowEvent e) {
        applet.start();
    }

    public void windowClosing(WindowEvent e) {
        applet.stop();
        applet.destroy();
        System.exit(0);
    }
}

public class SimFrame extends JFrame implements AppletStub, AppletContext {
    public SimFrame(JApplet applet) {
        setTitle("Физика v1.0a");
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension d = tk.getScreenSize();
        setSize(800, 500);
        setLocation(300 + d.width / 10, 200 + d.height / 10);
        setResizable(false);
        applet.setStub(this);
        addWindowListener(new SimWindowAdapter(applet));
    }

    public boolean isActive() {
        return true;
    }

    public URL getDocumentBase() {
        return null;
    }

    public URL getCodeBase() {
        return null;
    }

    public String getParameter(String name) {
        if (name.equalsIgnoreCase("simulation")) return "double spring";
        return null;
    }

    public AppletContext getAppletContext() {
        return this;
    }

    public void appletResize(int width, int height) {
    }

    public AudioClip getAudioClip(URL url) {
        return null;
    }

    public Image getImage(URL url) {
        return null;
    }

    public Applet getApplet(String name) {
        return null;
    }

    public Enumeration getApplets() {
        return null;
    }

    public void showDocument(URL url) {
    }

    public void showDocument(URL url, String target) {
    }

    public void showStatus(String status) {
    }

    public void setStream(String key, InputStream stream) throws IOException {
    }

    public InputStream getStream(String key) {
        return null;
    }

    public Iterator getStreamKeys() {
        return null;
    }

}
