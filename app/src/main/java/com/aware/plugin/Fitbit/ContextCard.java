package com.aware.plugin.fitbit;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.aware.utils.IContextCard;
import java.util.Timer;
import java.util.TimerTask;

public class ContextCard implements IContextCard{

    //Constructor used to instantiate this card
    public ContextCard() {
    }

    @Override
    public View getContextCard(Context context) {
        //Load card information to memory
        LayoutInflater sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View card = sInflater.inflate(R.layout.card, null);

        //Return the card to AWARE/apps
        return card;
    }
}