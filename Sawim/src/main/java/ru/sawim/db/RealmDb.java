package ru.sawim.db;

import android.content.Context;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmObject;

public class RealmDb {

    private static RealmConfiguration realmConfiguration;

    private RealmDb() {
    }

    public static void init(Context context) {
        realmConfiguration = new RealmConfiguration.Builder(context)
                .modules(new MyRealmModule())
                .name("sawim.realm")
                .schemaVersion(1)
                .build();
    }

    public static void save(final Object entity) {
        Realm realm = realm();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                if (entity instanceof Iterable) {
                    realm.copyToRealmOrUpdate((Iterable) entity);
                } else if (entity instanceof RealmObject) {
                    realm.copyToRealmOrUpdate((RealmObject) entity);
                } else {
                    throw new IllegalArgumentException("Entity must extend RealmObject");
                }
            }
        });
        realm.close();
    }

    public static Realm realm() {
        return Realm.getInstance(realmConfiguration);
    }
}
