package ru.sawim.roster;

/**
 * Created by gerc on 23.02.2015.
 */
public class Layer implements TreeNode {

    private int id;
    private String text;

    public Layer(String text, int id) {
        this.text = text;
        this.id = id;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public int getNodeWeight() {
        return id;
    }

    @Override
    public byte getType() {
        return LAYER;
    }

    @Override
    public int getGroupId() {
        return id;
    }
}
