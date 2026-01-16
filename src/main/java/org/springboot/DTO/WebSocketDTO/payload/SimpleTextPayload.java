package org.springboot.DTO.WebSocketDTO.payload;

public class SimpleTextPayload {
    private String Text;

    public SimpleTextPayload(String Text){
        this.Text = Text;
    }

    public String getText(){
        return this.Text;
    }
}