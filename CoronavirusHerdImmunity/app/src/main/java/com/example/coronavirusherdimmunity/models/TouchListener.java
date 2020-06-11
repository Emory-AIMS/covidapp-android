package com.example.coronavirusherdimmunity.models;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;

public class TouchListener {

    /**
     * allow to simulate click Button effect
     * change colour of Button when is pressed
     * create a list of colours based on these events:
     *      - a button when is pressed
     *      - a button when is normal
     *
     * @param view
     */
    public static void buttonClickEffect(View view)
    {
        Drawable drawableNormal = view.getBackground();

        Drawable drawablePressed = view.getBackground().getConstantState().newDrawable();
        drawablePressed.mutate();
        drawablePressed.setColorFilter(Color.argb(50, 255, 201, 187), PorterDuff.Mode.SRC_ATOP);

        StateListDrawable listDrawable = new StateListDrawable();
        listDrawable.addState(new int[] {android.R.attr.state_pressed}, drawablePressed);   //add colour when Button is pressed
        listDrawable.addState(new int[] {}, drawableNormal);                                //add colour when Button is normal
        view.setBackground(listDrawable);
    }


    /**
     * allow to simulate disabled button effect
     * change colour of Button when is pressed and disabled
     * create a list of colours based on these events:
     *      - a button when is pressed
     *      - a button when is disabled
     *      - a button when is normal
     * @param view
     */
    public static void buttonDisabledEffect(View view)
    {
        Drawable drawableNormal = view.getBackground();

        Drawable drawableDisabled = view.getBackground().getConstantState().newDrawable();
        drawableDisabled.mutate();
        drawableDisabled.setColorFilter(Color.argb(100, 255, 255, 255), PorterDuff.Mode.SRC_ATOP);

        Drawable drawablePressed = view.getBackground().getConstantState().newDrawable();
        drawablePressed.mutate();
        drawablePressed.setColorFilter(Color.argb(50, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);

        StateListDrawable listDrawable = new StateListDrawable();
        listDrawable.addState(new int[] {android.R.attr.state_pressed}, drawablePressed);     //add colour when Button is pressed
        listDrawable.addState(new int[] {-android.R.attr.state_enabled}, drawableDisabled);   //add colour when Button is disabled
        listDrawable.addState(new int[] {}, drawableNormal);                                  //add colour when Button is normal
        view.setBackground(listDrawable);
    }
}
