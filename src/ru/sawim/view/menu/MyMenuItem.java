package ru.sawim.view.menu;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 07.07.13
 * Time: 20:50
 * To change this template use File | Settings | File Templates.
 */
public class MyMenuItem {
    public String nameItem;
    public int idItem;

    public void addItem(String name, int id) {
        nameItem = name;
        idItem = id;
    }
}