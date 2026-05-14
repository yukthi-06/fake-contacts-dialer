package com.vypeensoft.fakecall;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<ContactModel> contactList;
    private List<ContactModel> contactListFiltered;
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(ContactModel contact);
    }

    public ContactAdapter(List<ContactModel> contactList, OnContactClickListener listener) {
        this.contactList = contactList;
        this.contactListFiltered = new ArrayList<>(contactList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactModel contact = contactListFiltered.get(position);
        holder.txtName.setText(contact.getName());
        holder.txtPhone.setText(contact.getPhone());
        
        // In a real app, we would load the photo here. 
        // For now, we use the placeholder.
        holder.imgContact.setImageResource(R.drawable.ic_person_placeholder);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactClick(contact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactListFiltered.size();
    }

    public void filter(String query) {
        contactListFiltered.clear();
        if (query.isEmpty()) {
            contactListFiltered.addAll(contactList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (ContactModel contact : contactList) {
                if (contact.getName().toLowerCase().contains(lowerCaseQuery) ||
                    contact.getPhone().contains(lowerCaseQuery)) {
                    contactListFiltered.add(contact);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtPhone;
        ImageView imgContact;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txt_name);
            txtPhone = itemView.findViewById(R.id.txt_phone);
            imgContact = itemView.findViewById(R.id.img_contact);
        }
    }
}
