/*
 * Copyright (C) 2016 Doodle AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.seraphim.chips;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

class ChipsEditText extends MaterialAutoCompleteTextView implements AdapterView.OnItemClickListener {
    private InputConnectionWrapperInterface inputConnectionWrapperInterface;
    private ItemClickListener itemClickListener;
    private ChipsAdapter adapter;

    public ChipsEditText(Context context, InputConnectionWrapperInterface inputConnectionWrapperInterface, final ItemClickListener itemClickListener) {
        super(context);
        this.inputConnectionWrapperInterface = inputConnectionWrapperInterface;
        this.itemClickListener = itemClickListener;
        adapter = new ChipsAdapter();
        setAdapter(adapter);
        setOnItemClickListener(this);
        setThreshold(1);
    }

    public void addSuggestions(List<ChipEntry> entries) {
        adapter.addSuggestions(entries);
    }

    public void setSuggestions(List<ChipEntry> entries) {
        adapter.setSuggestions(entries);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        if (inputConnectionWrapperInterface != null) {
            return inputConnectionWrapperInterface.getInputConnection(super.onCreateInputConnection(outAttrs));
        }

        return super.onCreateInputConnection(outAttrs);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        try {
            ChipEntry entry = (ChipEntry) parent.getItemAtPosition(position);
            setText("");
            itemClickListener.clicked(entry);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean enoughToFilter() {
        return getText().length() >= getThreshold();
    }

    public interface InputConnectionWrapperInterface {
        InputConnection getInputConnection(InputConnection target);
    }

    public interface ItemClickListener {
        void clicked(ChipEntry entry);
    }

    private class ChipsAdapter extends BaseAdapter implements Filterable {
        private final List<ChipEntry> suggestions;
        private final List<ChipEntry> currentEntries;

        public ChipsAdapter() {
            suggestions = new ArrayList<>();
            currentEntries = new ArrayList<>();
        }

        public void addSuggestions(List<ChipEntry> entries) {
            suggestions.addAll(entries);
            notifyDataSetChanged();
        }

        public void setSuggestions(List<ChipEntry> entries) {
            suggestions.clear();
            suggestions.addAll(entries);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return currentEntries.size();
        }

        @Override
        public Object getItem(int position) {
            return currentEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.material_list_item_with_avatar_1, parent, false);
            }
            ChipEntry chipEntry = currentEntries.get(position);
            Context context = convertView.getContext();
            ImageView imageView = (ImageView) convertView.findViewById(R.id.preview);
            if (chipEntry.avatarUri() != null) {
                Glide.with(context)
                        .load(chipEntry.avatarUri())
                        .asBitmap()
                        .transform(new CenterCrop(context))
                        .placeholder(R.color.paper)
                        .into(imageView);
            } else {
                Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_person_24dp);
                drawable.setAlpha(150);
                imageView.setImageDrawable(drawable);
                imageView.setPadding(8, 8, 8, 8);
            }
            ((TextView) convertView.findViewById(R.id.primary_text)).setText(chipEntry.displayedName());
            return convertView;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                private String lastFiltered;

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null && !constraint.toString().equals(lastFiltered) && constraint.length() != 0) {
                        List<ChipEntry> entries = new ArrayList<>();
                        for (ChipEntry entry : suggestions) {
                            if (entry.displayedName().contains(constraint))
                                entries.add(entry);
                        }
                        filterResults.values = entries;
                        filterResults.count = entries.size();
                        lastFiltered = constraint.toString();
                    } else {
                        filterResults.values = suggestions;
                        filterResults.count = suggestions.size();
                        if (constraint != null) lastFiltered = constraint.toString();
                        else lastFiltered = null;
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        currentEntries.addAll((List<ChipEntry>) results.values);
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
        }
    }
}