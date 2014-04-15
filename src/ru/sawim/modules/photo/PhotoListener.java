package ru.sawim.modules.photo;


import ru.sawim.activities.BaseActivity;

public interface PhotoListener {
    void processPhoto(BaseActivity activity, byte[] photo);
}


