package com.example.finalyearproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalyearproject.Classes.Patient;

import java.util.ArrayList;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientHolder>{
    private ArrayList<Patient> patients,copy = new ArrayList<>();;
    private Context context;

    public PatientAdapter(ArrayList<Patient> patients, Context context)
    {
        this.patients = patients;
        this.context = context;
        copy.addAll(patients);
    }

    @NonNull
    @Override
    public PatientHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.patient_card,parent,false);
        return new PatientHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientHolder holder, int position) {
        Patient patient = patients.get(position);
        holder.setDetails(patient);
    }

    @Override
    public int getItemCount() {
        return patients==null ? 0 : patients.size();
    }

    public void filter(String s) {
            patients.clear();
            if (s.equals("")) {
                patients.addAll(copy);
            } else {
                for (Patient patient : copy) {
                    System.out.println(patient.getName());
                    if (patient.getName().toLowerCase().contains(s.toLowerCase())) {
                        System.out.println(s);
                        patients.add(patient);
                    }
                }
            }

            notifyDataSetChanged();

    }

    public onClickPatient monClickPatient;

    public void setMonClickPatient(onClickPatient monClickPatient) {
        this.monClickPatient = monClickPatient;
    }

    public interface onClickPatient{
        void clicked(int position);
    }

    class PatientHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView patientNameTxt;
        CardView patientCard;
        ImageView availableImage;
        public PatientHolder(@NonNull View itemView) {
            super(itemView);
            patientNameTxt = itemView.findViewById(R.id.PatientNameTxt);
            patientCard = itemView.findViewById(R.id.PatientCard);
            patientCard.setOnClickListener(this);
            availableImage = itemView.findViewById(R.id.statusImage);
        }

        public void setDetails(Patient patient) {
            patientNameTxt.setText(patient.getName());
            if(patient.isAvailable())
                availableImage.setImageResource(R.drawable.ok);
            else
                availableImage.setImageResource(R.drawable.notok);
        }

        @Override
        public void onClick(View v) {
            if(monClickPatient!=null)
                monClickPatient.clicked(getAdapterPosition());
        }
    }
}
