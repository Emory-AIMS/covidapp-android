package com.example.coronavirusherdimmunity.resourceprovider;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

public interface ResourceProvider {

    String getString(@StringRes int stringId);

    int getColor(@ColorRes int colorId);

}