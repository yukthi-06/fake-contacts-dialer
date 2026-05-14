package com.vypeensoft.fakecall;

import java.io.Serializable;

public class ContactModel implements Serializable {
    private String name;
    private String phone;
    private String photo;
    private String audio;

    public ContactModel(String name, String phone, String photo, String audio) {
        this.name = name;
        this.phone = phone;
        this.photo = photo;
        this.audio = audio;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }
}
