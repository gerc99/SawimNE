package ru.sawim.models;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by gerc on 10.08.2014.
 */
public class CustomPagerAdapter extends PagerAdapter {
    private List<View> pagesList;

    public CustomPagerAdapter(List<View> pages) {
        pagesList = pages;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View page = pagesList.get(position);
        if (container.indexOfChild(page) == -1) {
            container.addView(page);
        }
        return page;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title = "";
        if (pagesList.size() > 0)
            title = (String) pagesList.get(position).getTag();
        return title;
    }

    @Override
    public int getCount() {
        return pagesList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}
