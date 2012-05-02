/* This file was automatically generated by TightDB. */

package com.tightdb.generated;


import com.tightdb.*;
import com.tightdb.lib.*;

/**
 * This class represents a TightDB cursor and was automatically generated.
 */
public class Employee extends AbstractCursor<Employee> {

    public final StringColumn<Employee, EmployeeQuery> firstName;
    public final StringColumn<Employee, EmployeeQuery> lastName;
    public final LongColumn<Employee, EmployeeQuery> salary;
    public final BooleanColumn<Employee, EmployeeQuery> driver;
    public final BinaryColumn<Employee, EmployeeQuery> photo;
    public final DateColumn<Employee, EmployeeQuery> birthdate;
    public final MixedColumn<Employee, EmployeeQuery> extra;

	public Employee(TableBase table, long position) {
		super(table, Employee.class, position);

        firstName = new StringColumn<Employee, EmployeeQuery>(table, this, 0, "firstName");
        lastName = new StringColumn<Employee, EmployeeQuery>(table, this, 1, "lastName");
        salary = new LongColumn<Employee, EmployeeQuery>(table, this, 2, "salary");
        driver = new BooleanColumn<Employee, EmployeeQuery>(table, this, 3, "driver");
        photo = new BinaryColumn<Employee, EmployeeQuery>(table, this, 4, "photo");
        birthdate = new DateColumn<Employee, EmployeeQuery>(table, this, 5, "birthdate");
        extra = new MixedColumn<Employee, EmployeeQuery>(table, this, 6, "extra");
	}

	public java.lang.String getFirstName() {
		return this.firstName.get();
	}

	public void setFirstName(java.lang.String firstName) {
		this.firstName.set(firstName);
	}

	public java.lang.String getLastName() {
		return this.lastName.get();
	}

	public void setLastName(java.lang.String lastName) {
		this.lastName.set(lastName);
	}

	public long getSalary() {
		return this.salary.get();
	}

	public void setSalary(long salary) {
		this.salary.set(salary);
	}

	public boolean getDriver() {
		return this.driver.get();
	}

	public void setDriver(boolean driver) {
		this.driver.set(driver);
	}

	public byte[] getPhoto() {
		return this.photo.get();
	}

	public void setPhoto(byte[] photo) {
		this.photo.set(photo);
	}

	public java.util.Date getBirthdate() {
		return this.birthdate.get();
	}

	public void setBirthdate(java.util.Date birthdate) {
		this.birthdate.set(birthdate);
	}

	public java.io.Serializable getExtra() {
		return this.extra.get();
	}

	public void setExtra(java.io.Serializable extra) {
		this.extra.set(extra);
	}

}