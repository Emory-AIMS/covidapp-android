package com.example.coronavirusherdimmunity.resourceprovider;

import android.content.Context;

import androidx.core.content.ContextCompat;

public class ResourceProviderDefault implements ResourceProvider {
    private Context context;

    public ResourceProviderDefault(Context context) {
        this.context = context;
    }

    @Override
    public String getString(int stringId) {
        return context.getString(stringId);
    }

    @Override
    public int getColor(int colorId) {
        return ContextCompat.getColor(context, colorId);
    }
}
