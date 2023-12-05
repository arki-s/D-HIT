package analix.DHIT.model;

import java.util.List;

public class User {
    private int employeeCode;
    private String name;
    private String password;
    private String role;
    private String icon;

    public int getEmployeeCode()
    {
        return employeeCode;
    }
    public void setEmployeeCode(int employeeCode)
    {
        this.employeeCode = employeeCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<String> roleList() {
        return List.of(getRole());
    }
}