package iut_lry.coursedorientation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import android.content.Context;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;


public class TabFragment2 extends Fragment implements View.OnClickListener {
    private IFragmentToActivity mCallback;

    private Button scanButton;
    Calendar rightNow;
    Chronometer timeElapsed;

    DBController controller;

    boolean departOK;

    String scanContent;
    String scanFormat;
    String temps;
    int resultat;

    TextView totalBalises;
    ProgressBar progressTotal;
    int[] scannéSurTotal;

    TextView baliseDepart;
    String nbBaliseDepart;
    TextView baliseArrivee;
    String nbBaliseArrivee;

    TextView points;
    String nbPoints;

    TextView txtBalisePointee;
    TextView txtBaliseSuivante;
    String baliseSuivante;
    String nbBaliseSuivante;
    TextView balisePoche;
    String baliseRemainingPoche;
    String baliseCheckedPoche;
    String baliseSortiePoche;

    TextView baliseLiaisons;

    String[] baliseActuelle;

    LinearLayout interface2;
    TextView noParcours2;

    LinearLayout startBalise;
    LinearLayout endBalise;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_fragment_2, container, false);

        scanButton = (Button) view.findViewById(R.id.scan_button);
        scanButton.setOnClickListener(this);

        controller = new DBController(getActivity());

        totalBalises = (TextView) view.findViewById(R.id.textView_total_balises_nb);
        progressTotal = (ProgressBar) view.findViewById(R.id.progressBarTotal);

        baliseDepart = (TextView) view.findViewById(R.id.textView_balise_depart_nb);
        baliseArrivee = (TextView) view.findViewById(R.id.textView_balise_arrivee_nb);
        points = (TextView) view.findViewById(R.id.textView_points_nb);
        txtBalisePointee = (TextView) view.findViewById(R.id.textView_balise_pointee_nb);
        txtBaliseSuivante = (TextView) view.findViewById(R.id.textView_balise_suivante_nb);
        balisePoche = (TextView) view.findViewById(R.id.textView_poche_nb);
        baliseLiaisons = (TextView) view.findViewById(R.id.textView_liaisons_nb);

        interface2 = (LinearLayout) view.findViewById(R.id.interface2);
        noParcours2 = (TextView) view.findViewById(R.id.noParcours2);

        startBalise = (LinearLayout) view.findViewById(R.id.startBalise);
        endBalise = (LinearLayout) view.findViewById(R.id.endBalise);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        timeElapsed  = (Chronometer) getActivity().findViewById(R.id.chronometer1);
        timeElapsed.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener(){
            @Override
            public void onChronometerTick(Chronometer cArg) {
                long time = SystemClock.elapsedRealtime() - cArg.getBase();
                int h   = (int)(time /3600000);
                int m = (int)(time - h*3600000)/60000;
                int s= (int)(time - h*3600000- m*60000)/1000 ;
                String hh = h < 10 ? "0"+h: h+"";
                String mm = m < 10 ? "0"+m: m+"";
                String ss = s < 10 ? "0"+s: s+"";
                cArg.setText(hh+":"+mm+":"+ss);
            }
        });
        timeElapsed.setText("00:00:00");
        departOK = false;

        fragmentCommunication2();
        //vérif tableau.

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.scan_button){

            //On lance le scanner au clic sur notre bouton
            IntentIntegrator.forSupportFragment(this)
                    .setCaptureActivity(CaptureActivityAnyOrientation.class)
                    .setPrompt("Encadrez le code-barres d'une balise pour la valider !")
                    .setBeepEnabled(false)
                    .setOrientationLocked(false).initiateScan();
        }
    }

    // Get the results:
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(getActivity(), "Scan annulé !", Toast.LENGTH_LONG).show();

                //TODO jouez son echec scan

            }
            else {

                //Récupération du contenu du scan
                scanContent = result.getContents();
                scanFormat = result.getFormatName();

                /*
                //récupère le temps actuel SERT PLUS A RIEN
                rightNow = Calendar.getInstance();
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                temps = format.format(rightNow.getTime());
                */

                if(scanContent.equals("BALISE TEST")) {
                    mCallback.showToast("La balise TEST a été scanné !","court");
                } else {
                    ReceiveData test = new ReceiveData();
                    test.execute();
                }


                /*//Joué un son -> BUG
                final MediaPlayer mp = MediaPlayer.create(getActivity(), R.raw.zxing_beep);
                mp.setVolume(10,10);
                mp.start();*/

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallback = (IFragmentToActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement IFragmentToActivity");
        }
    }

    public class ReceiveData extends AsyncTask<String, Integer, Void> {

        @Override
        protected void onPreExecute() {
        /*
         *    do things before doInBackground() code runs
         *    such as preparing and showing a Dialog or ProgressBar
        */
            //empêcher un autre scan en même temps.
            scanButton.setEnabled(false);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        /*
         *    updating data
         *    such a Dialog or ProgressBar
        */
            if(values[0] == 1) {
                //Si on fait ca, on a le temps quand on appuie sur le bouton SCAN
                //et pas quand on obtient le résultat
                //temps = timeElapsed.getText().toString();
                //Donc obligé de refaire ça :
                long time = SystemClock.elapsedRealtime() - timeElapsed.getBase();
                int h   = (int)(time /3600000);
                int m = (int)(time - h*3600000)/60000;
                int s= (int)(time - h*3600000- m*60000)/1000 ;
                String hh = h < 10 ? "0"+h: h+"";
                String mm = m < 10 ? "0"+m: m+"";
                String ss = s < 10 ? "0"+s: s+"";
                temps = hh+":"+mm+":"+ss;

                Toast.makeText(getActivity(), "La balise n°" + scanContent + " a été scanné ! " + temps, Toast.LENGTH_LONG).show();
            }
            else if(values[0] == 3)
            {
                temps = timeElapsed.getText().toString();
                Toast.makeText(getActivity(), "La balise n°" + scanContent + " a été scanné ! " + temps, Toast.LENGTH_LONG).show();

                timeElapsed.setBase(SystemClock.elapsedRealtime());
                timeElapsed.start();
                departOK = true;
            }
            else if(values[0] == 2)
            {
                Toast.makeText(getActivity(), "La balise n°" + scanContent + " a déjà été scanné !", Toast.LENGTH_LONG).show();
            }
            else if(values[0] == 4)
            {
                Toast.makeText(getActivity(), "La balise n°" + scanContent + " n'est pas la balise de départ !", Toast.LENGTH_LONG).show();
            }
            else if(values[0] == 5)
            {
                Toast.makeText(getActivity(), "La balise n°" + scanContent + " n'est pas la balise suivante !", Toast.LENGTH_LONG).show();
            }
            else if(values[0] == 6)
            {
                Toast.makeText(getActivity(), "Vous avez déjà fini le parcours!", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(getActivity(), "La balise n°" + scanContent + " n'est pas dans le parcours !", Toast.LENGTH_LONG).show();
            }

        }

        @Override
        protected Void doInBackground(String... parametres) {
            //do your work here

            resultat = controller.checkBalise(scanContent, temps, departOK, nbBaliseDepart, baliseSuivante, nbBaliseSuivante);

            publishProgress(resultat);


            //On doit attendre pour que le programme ait le temps de mettre à jour la variable temps dans onProgressUpdate
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }



            if(resultat == 1 || resultat == 3)
            {
                //Update du temps dans la base de données
                controller.UpdateTemps(scanContent,temps);

                //pour pouvoir vérifier si la suivante a déjà été pointé
                //mettre à jour la dernière balise pointée et sa suivante
                baliseActuelle = controller.getBaliseActuelle();
                //stocker la variable pour vérifier quand on scanne.
                nbBaliseSuivante = baliseActuelle[2];
                controller.updateNextAlreadyChecked(nbBaliseSuivante);

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        /*
         *    do something with data here
         *    display it or send to mainactivity
         *    close any dialogs/ProgressBars/etc...
        */
            //Affichage dans les TextViews
            TextView scan_format = (TextView) getActivity().findViewById(R.id.scan_format);
            TextView scan_content = (TextView) getActivity().findViewById(R.id.scan_content);

            scan_format.setText("FORMAT: " + scanFormat);
            scan_content.setText("CONTENT: " + scanContent);

            if(resultat == 1 || resultat == 3)
            {
                //On update la listView du fragment 3 (onglet parcours)
                mCallback.communicateToFragment3();
                //on update les infos de l'onglet "infos"
                updateInfos();

            }
            //au hasard
            resultat = 54;

            //réactiver le bouton
            scanButton.setEnabled(true);

        }
    }

    @Override
    public void onDetach() {
        mCallback = null;
        super.onDetach();
    }

    public void onRefresh() {
        Toast.makeText(getActivity(), "Fragment 2: Refresh called.",
                Toast.LENGTH_SHORT).show();
    }

    public void updateInfos() {

        UpdateInfosThread thread = new UpdateInfosThread();
        thread.execute();

    }

    public class UpdateInfosThread extends AsyncTask<String, Integer, Void> {

        @Override
        protected void onPreExecute() {
        /*
         *    do things before doInBackground() code runs
         *    such as preparing and showing a Dialog or ProgressBar
        */

        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        /*
         *    updating data
         *    such a Dialog or ProgressBar
        */


        }

        @Override
        protected Void doInBackground(String... parametres) {
            //do your work here

            //on récupère les données dans la base
            scannéSurTotal = controller.getNbCheckpoints();

            //Afficher soit la balise de départ avant le départ et la balise d'arrivée pendant la course
            if(departOK){
                nbBaliseArrivee = controller.getLastBalise();

            } else {
                nbBaliseDepart = controller.getFirstBalise();
            }

            //mettre à jour la dernière balise pointée et sa suivante
            baliseActuelle = controller.getBaliseActuelle();

            //poche
            baliseSortiePoche = controller.getSortiePoche(baliseActuelle[6]);
            baliseRemainingPoche = controller.getRemainingPoche(baliseActuelle[6]);
            baliseCheckedPoche = controller.getCheckedPoche(baliseActuelle[6]);


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        /*
         *    do something with data here
         *    display it or send to mainactivity
         *    close any dialogs/ProgressBars/etc...
        */

            //on met *100 pour voir l'animation
            progressTotal.setMax(scannéSurTotal[1] * 100);
            //sans animation
            //progressTotal.setProgress(scannéSurTotal[0]);
            //avec animation
            ObjectAnimator animation = ObjectAnimator.ofInt(progressTotal, "progress", scannéSurTotal[0]*100);
            animation.setDuration(600); // 1.5 second
            animation.setInterpolator(new LinearInterpolator());
            animation.start();

            //pour mettre à jour le numéro à la fin de l'animation
            animation.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    totalBalises.setText(scannéSurTotal[0] + "/" + scannéSurTotal[1]);
                }
            });

            //Afficher soit la balise de départ avant le départ et la balise d'arrivée pendant la course
            if(departOK){
                baliseArrivee.setText(nbBaliseArrivee);
                //cacher la balise de départ et afficher celle d'arrivee
                startBalise.setVisibility(View.GONE);
                endBalise.setVisibility(View.VISIBLE);

            } else {
                baliseDepart.setText(nbBaliseDepart);
                //cacher la balise d'arrivee et afficher celle de départ
                startBalise.setVisibility(View.VISIBLE);
                endBalise.setVisibility(View.GONE);
            }

            //dernière balise
            txtBalisePointee.setText(baliseActuelle[0]);
            //balise suivante
            if((baliseActuelle[1].equals("obligatoire") || baliseActuelle[1].equals("optionnelle")) && baliseActuelle[3].equals("non"))
            {
                txtBaliseSuivante.setText(baliseActuelle[2] + " (" + baliseActuelle[1] + ")");
            }
            else if(baliseActuelle[3].equals("oui"))
            {
                //Quand prochaine est azimut
                txtBaliseSuivante.setText("Azimut " + baliseActuelle[4] + "° " + baliseActuelle[5] + "m" + " (" + baliseActuelle[1] + ")");
                Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                v.vibrate(500);
            }
            else
            {
                txtBaliseSuivante.setText(baliseActuelle[1]);
            }
            //poche
            TextView textView_poche = (TextView) getActivity().findViewById(R.id.textView_poche);
            if(baliseActuelle[6].equals("")) { //si on vient de dll le parkour
                textView_poche.setText("Poche actuelle");
                balisePoche.setText("\n");
            } else if(!baliseActuelle[6].equals("null")){ //si la balise actuelle fait partie d'une poche
                textView_poche.setText("Poche actuelle : " + baliseActuelle[6]);
                balisePoche.setTextSize(15);
                balisePoche.setText("sortie : " + baliseSortiePoche
                                  + "\nrestantes : " + baliseRemainingPoche
                                  + "\nscannées : " + baliseCheckedPoche);
            } else { //si la balise actuelle n'a pas de poche
                textView_poche.setText("Poche actuelle");
                balisePoche.setTextSize(25);
                balisePoche.setText("aucune\n");
            }

            //stocker la variable pour vérifier quand on scanne.
            baliseSuivante = baliseActuelle[1];
            nbBaliseSuivante = baliseActuelle[2];


            //regarder si la balise est la dernière.
            if(baliseActuelle[1].equals("aucune") && departOK){
                timeElapsed.stop();
                departOK=false;
                mCallback.showToast("Vous avez scanné la balise de fin !","long");
            }

        }
    }

    public void fragmentCommunication2() {

        ArrayList<HashMap<String, String>> baliseList;
        // Get User records from SQLite DB

        baliseList = controller.getAllBalises();

        timeElapsed.stop();
        timeElapsed.setText("00:00:00");

        if (baliseList.size() != 0) {

            //afficher l'interface du deuxieme onglet et cacher la phrase
            interface2.setVisibility(LinearLayout.VISIBLE);
            noParcours2.setVisibility(LinearLayout.GONE);

            //on check si la balise depart a déjà été scanné, si oui on met departOK a true.
            departOK = controller.checkFirstBalise();

            //si la base de données est déjà remplie, on update les infos
            updateInfos();

            //fonction a mettre en place pour récupérer le bon chrono si l'activité s'arrete.
        }
        else
        {
            //timeElapsed.stop();
            //timeElapsed.setText("00:00:00");
            departOK = false;

            //cacher l'interface du deuxieme onglet et afficher la phrase
            interface2.setVisibility(LinearLayout.GONE);
            noParcours2.setVisibility(LinearLayout.VISIBLE);
        }
    }

}