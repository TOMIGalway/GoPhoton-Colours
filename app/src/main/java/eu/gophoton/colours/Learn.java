package eu.gophoton.colours;

import eu.gophoton.colours.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
public class Learn extends Fragment {
   @Override
      public View onCreateView(LayoutInflater inflater, ViewGroup container,
              Bundle savedInstanceState) {
          View ios = inflater.inflate(R.layout.learn_frag, container, false);
          ((TextView)ios.findViewById(R.id.textView2)).setText(R.string.Learn);
          return ios;
}}