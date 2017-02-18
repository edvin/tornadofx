package tornadofx.tests;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public class JavaPerson {

    private PropertyChangeSupport pcs;
    private Integer id;
    private String name;
    private String department;
    private String primaryEmail;
    private String secondaryEmail;
    private List<JavaPerson> employees = new ArrayList<>();

    public JavaPerson(){
        pcs = new PropertyChangeSupport(this);
    }

    public JavaPerson( String name, String department, String primaryEmail, String secondaryEmail ){
        this( name, department, primaryEmail, secondaryEmail, null );
    }

    public JavaPerson( String name, String department, String primaryEmail, String secondaryEmail, List<JavaPerson> employees ){
        this.name = name;
        this.department = department;
        this.primaryEmail = primaryEmail;
        this.secondaryEmail = secondaryEmail;
        this.employees = employees;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPrimaryEmail() {
        return primaryEmail;
    }

    public void setPrimaryEmail(String primaryEmail) {
        this.primaryEmail = primaryEmail;
    }

    public String getSecondaryEmail() {
        return secondaryEmail;
    }

    public void setSecondaryEmail(String secondaryEmail) {
        this.secondaryEmail = secondaryEmail;
    }

    public List<JavaPerson> getEmployees() {
        return employees;
    }

    public void setEmployees(List<JavaPerson> employees) {
        this.employees = employees;
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
