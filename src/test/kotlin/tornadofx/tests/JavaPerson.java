package tornadofx.tests;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class JavaPerson {

    private PropertyChangeSupport pcs;
    private Integer id;
    private String name;

    public JavaPerson(){
        pcs = new PropertyChangeSupport(this);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        Integer oldValue = this.id;
        this.id = id;
        pcs.firePropertyChange("id",oldValue,id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldValue = this.name;
        this.name = name;
        pcs.firePropertyChange("name",oldValue,name);
    }

    public String toString() {
        return "JavaPerson{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }


}
