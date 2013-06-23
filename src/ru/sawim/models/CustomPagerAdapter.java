package ru.sawim.models;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 26.04.13
 * Time: 21:02
 * To change this template use File | Settings | File Templates.
 */
public class CustomPagerAdapter extends PagerAdapter {

    private List<View> pagesList;

    public CustomPagerAdapter(List<View> pages) {
        pagesList = pages;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View page = pagesList.get(position);
        container.addView(page);
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