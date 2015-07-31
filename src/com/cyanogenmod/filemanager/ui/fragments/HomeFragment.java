/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.filemanager.ui.fragments;

import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import com.cyanogenmod.filemanager.R;
import com.cyanogenmod.filemanager.activities.MainActivity;
import com.cyanogenmod.filemanager.activities.MainActivity.FragmentType;
import com.cyanogenmod.filemanager.activities.SearchActivity;
import com.cyanogenmod.filemanager.model.FileSystemObject;
import com.cyanogenmod.filemanager.ui.IconHolder;
import com.cyanogenmod.filemanager.util.FileHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper;
import com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.APP;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.AUDIO;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.DOCUMENT;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.IMAGE;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.COMPRESS;
import static com.cyanogenmod.filemanager.util.MimeTypeHelper.MimeTypeCategory.VIDEO;

public class HomeFragment extends Fragment {

    View mView;
    Toolbar mToolBar;
    SearchView mSearchView;
    private ArrayAdapter<MimeTypeCategory> mQuickSearchAdapter;
    private static final List<MimeTypeCategory> QUICK_SEARCH_LIST
            = new ArrayList<MimeTypeCategory>() {
        {
            add(IMAGE);
            add(AUDIO);
            add(VIDEO);
            add(DOCUMENT);
            add(APP);
            add(COMPRESS);
        }
    };

    static Map<MimeTypeCategory, Integer> QUICK_SEARCH_ICONS
            = new HashMap<MimeTypeCategory, Integer>();
    static {
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.IMAGE, R.drawable.ic_category_images);
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.AUDIO, R.drawable.ic_category_audio);
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.VIDEO, R.drawable.ic_category_video);
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.DOCUMENT, R.drawable.ic_category_docs);
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.APP, R.drawable.ic_category_apps);
        QUICK_SEARCH_ICONS.put(MimeTypeCategory.COMPRESS, R.drawable.ic_category_archives);
    }

    static String MIME_TYPE_LOCALIZED_NAMES[];
    private OnClickListener mQuickSearchItemClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Integer position = (Integer) view.getTag();
            onClicked(position);
        }
    };
    private IconHolder mIconHolder;

    LayoutInflater mLayoutInflater;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static HomeFragment newInstance() {
        HomeFragment frag = new HomeFragment();
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mLayoutInflater = inflater;
        mIconHolder = new IconHolder(getActivity(), false);
        mView = inflater.inflate(R.layout.home_fragment, container, false);

        final CardView cV = (CardView)mView.findViewById(R.id.add_provider);
        cV.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).setCurrentFragment(FragmentType.LOGIN);
            }
        });

        Button dismiss =(Button) mView.findViewById(R.id.dismiss_card);
        dismiss.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                cV.setVisibility(View.GONE);
                // TODO: Save that the card has been dismissed
            }
        });

        handleSearchBar();

        return mView;
    }

    private void handleSearchBar() {
        mSearchView = (SearchView) mView.findViewById(R.id.homepage_search_bar);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context
                .SEARCH_SERVICE);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity()
                .getComponentName()));
        mSearchView.setIconifiedByDefault(false);

        Resources res = mSearchView.getContext().getResources();
        int searchPlateId = res.getIdentifier("android:id/search_plate", null, null);
        View searchPlate = mSearchView.findViewById(searchPlateId);
        if (searchPlate != null) {
            int searchTextId = res.getIdentifier("android:id/search_src_text", null, null);
            TextView searchText = (TextView) searchPlate.findViewById(searchTextId);
            if (searchText != null) {
                int searchColor = res.getColor(R.color.search_bar_hint_text_color);
                searchText.setTextColor(searchColor);
                searchText.setHintTextColor(searchColor);
            }

            // Update all the image views to our assets
            int imageViewId = res.getIdentifier("android:id/search_button", null, null);
            ImageView imageView = (ImageView) mSearchView.findViewById(imageViewId);
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_search);
            }
            imageViewId = res.getIdentifier("android:id/search_mag_icon", null, null);
            imageView = (ImageView) mSearchView.findViewById(imageViewId);
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_search);
            }
            imageViewId = res.getIdentifier("android:id/search_voice_btn", null, null);
            imageView = (ImageView) mSearchView.findViewById(imageViewId);
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_search_voice);
            }
            imageViewId = res.getIdentifier("android:id/search_close_btn", null, null);
            imageView = (ImageView) mSearchView.findViewById(imageViewId);
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_cancel_close);
            }
        }

        mSearchView.setFocusable(false);
    }

    @Override
    public void onStart() {
        super.onStart();

        mToolBar = (Toolbar) mView.findViewById(
                R.id.material_toolbar);
        ActionBarActivity actionBarActivity = (ActionBarActivity) getActivity();
        actionBarActivity.setSupportActionBar(mToolBar);
        actionBarActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        actionBarActivity.getSupportActionBar().setHomeButtonEnabled(true);
        actionBarActivity.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);

        initQuickSearch();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    private void initQuickSearch() {
        MIME_TYPE_LOCALIZED_NAMES = MimeTypeCategory.getDefinedLocalizedNames(getActivity());
        GridView gridview = (GridView) mView.findViewById(R.id.quick_search_view);

        mQuickSearchAdapter = new ArrayAdapter<MimeTypeCategory>(getActivity(), R.layout
                .quick_search_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                convertView = (convertView == null) ?mLayoutInflater.inflate(
                        R.layout.quick_search_item, parent, false) : convertView;

                MimeTypeCategory item = getItem(position);
                String typeTitle = MIME_TYPE_LOCALIZED_NAMES[item.ordinal()];
                TextView typeTitleTV = (TextView) convertView
                        .findViewById(R.id.navigation_view_item_name);
                ImageView typeIconIV = (ImageView) convertView
                        .findViewById(R.id.navigation_view_item_icon);

                /*ColorStateList colorList = new ColorStateList(
                        new int[][]{new int[]{},
                                new int[]{}},
                        new int[]{0,
                                MimeTypeHelper.getIconColorFromIconId(getContext(),
                                        QUICK_SEARCH_ICONS.get(item))}); */

                //mIconHolder.loadDrawable(typeIconIV, null, QUICK_SEARCH_ICONS.get(item));


                typeTitleTV.setText(typeTitle);
                convertView.setOnClickListener(mQuickSearchItemClickListener);
                convertView.setTag(position);

                setFileIcon(typeIconIV, QUICK_SEARCH_ICONS.get(item), null);


                return convertView;
            }
        };
        mQuickSearchAdapter.addAll(QUICK_SEARCH_LIST);
        gridview.setAdapter(mQuickSearchAdapter);

        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Toast.makeText(getActivity(), "" + position, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setFileIcon(ImageView view, final int iconId, FileSystemObject fso) {
        // Use iconholder to check for thumbnail
        final IconHolder.ICallback callback = new IconHolder.ICallback() {
            @Override
            public void onLoaded(ImageView imageView, Drawable icon) {
                if (icon == null) {
                    // Icon holder didn't have anything at the moment, set default.
                    int colorId = MimeTypeHelper.getIconColorFromIconId(getActivity(), iconId);
                    setIcon(imageView, getResources().getDrawable(iconId),
                            R.color.navigation_view_icon_unselected, R.drawable.ic_icon_background,
                            getResources().getColor(colorId));

                }
            }
        };
        mIconHolder.loadDrawable(view, fso, iconId, callback);
    }

    // Set drawable as icon
    private void setIcon(ImageView view, Drawable iconDrawable, int iconColorId, int backgroundId,
                         int backgroundColor) {

        StateListDrawable stateListDrawable = new StateListDrawable();
        addUnselected(getResources(), stateListDrawable, iconDrawable, iconColorId);

        ColorStateList colorList = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_selected},
                        new int[]{}},
                new int[]{getResources().getColor(R.color.navigation_view_icon_selected),
                        backgroundColor});

        view.setBackgroundResource(backgroundId);
        view.setBackgroundTintList(colorList);
        view.setImageDrawable(stateListDrawable);
    }

    // default
    private void addUnselected(Resources res, StateListDrawable drawable, Drawable iconDrawable,
                               int colorId) {
        iconDrawable.setTint(res.getColor(colorId));
        drawable.addState(new int[0], iconDrawable);
    }

    private void onClicked(int position) {
        Intent intent = new Intent(getActivity(), SearchActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchActivity.EXTRA_SEARCH_DIRECTORY, FileHelper.ROOT_DIRECTORY);
        intent.putExtra(SearchManager.QUERY, "*"); // Use wild-card '*'

        if (position == 0) {
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

            fragmentManager.beginTransaction()
                    .replace(R.id.navigation_fragment_container, new NavigationFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            return;

        } else {
            ArrayList<MimeTypeCategory> searchCategories = new ArrayList<MimeTypeCategory>();
            MimeTypeCategory selectedCategory = QUICK_SEARCH_LIST.get(position);
            searchCategories.add(selectedCategory);
            // a one off case where we implicitly want to also search for TEXT mimetypes when the
            // DOCUMENTS category is selected
            if (selectedCategory == MimeTypeCategory.DOCUMENT) {
                searchCategories.add(
                        MimeTypeCategory.TEXT);
            }
            intent.putExtra(SearchActivity.EXTRA_SEARCH_MIMETYPE, searchCategories);
        }

        startActivity(intent);
    }
}