package edu.gkg.model;

public class Person {
    private Long personId;
    private String name;

    public Person() {}

    public Person(String name) {
        this.name = name;
    }

    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name;
    }
}