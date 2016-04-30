package ru.sawim.modules.photo;


import ru.sawim.ui.activity.BaseActivity;

public interface PhotoListener {
    void processPhoto(BaseActivity activity, byte[] photo);
}


