package material.hunter;

import static material.hunter.R.id.f_materialhunter_action_search;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import material.hunter.RecyclerViewAdapter.MaterialHunterRecyclerViewAdapter;
import material.hunter.RecyclerViewAdapter.MaterialHunterRecyclerViewAdapterDeleteItems;
import material.hunter.RecyclerViewData.MaterialHunterData;
import material.hunter.SQL.MaterialHunterSQL;
import material.hunter.models.MaterialHunterModel;
import material.hunter.utils.NhPaths;
import material.hunter.viewmodels.MaterialHunterViewModel;

public class MaterialHunterFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static int targetPositionId;
    private Context context;
    private Activity activity;
    private MaterialHunterRecyclerViewAdapter materialhunterRecyclerViewAdapter;
    private Button addButton;
    private Button deleteButton;
    private Button moveButton;

    public static MaterialHunterFragment newInstance(int sectionNumber) {
        MaterialHunterFragment fragment = new MaterialHunterFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.context = getContext();
        this.activity = getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.materialhunter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialHunterViewModel materialhunterViewModel = ViewModelProviders.of(this).get(MaterialHunterViewModel.class);
        materialhunterViewModel.init(context);
        materialhunterViewModel.getLiveDataMaterialHunterModelList().observe(getViewLifecycleOwner(), materialhunterModelList -> {
            materialhunterRecyclerViewAdapter.notifyDataSetChanged();
        });

        materialhunterRecyclerViewAdapter = new MaterialHunterRecyclerViewAdapter(context, materialhunterViewModel.getLiveDataMaterialHunterModelList().getValue());
        RecyclerView itemRecyclerView = view.findViewById(R.id.f_materialhunter_recyclerview);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        itemRecyclerView.setLayoutManager(linearLayoutManager);
        itemRecyclerView.setAdapter(materialhunterRecyclerViewAdapter);

        addButton = view.findViewById(R.id.f_materialhunter_addItemButton);
        deleteButton = view.findViewById(R.id.f_materialhunter_deleteItemButton);
        moveButton = view.findViewById(R.id.f_materialhunter_moveItemButton);

        // MaterialFeatures
        SwipeRefreshLayout o = view.findViewById(R.id.f_materialhunter_scrollView);
        o.setOnRefreshListener(() -> {
            MaterialHunterData.getInstance().refreshData();
            new Handler().postDelayed(() -> o.setRefreshing(false), 512);
        });

        onAddItemSetup();
        onDeleteItemSetup();
        onMoveItemSetup();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.materialhunter, menu);
        final MenuItem searchItem = menu.findItem(f_materialhunter_action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnSearchClickListener(v -> menu.setGroupVisible(R.id.f_materialhunter_menu_group1, false));
        searchView.setOnCloseListener(() -> {
            menu.setGroupVisible(R.id.f_materialhunter_menu_group1, true);
            return false;
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                materialhunterRecyclerViewAdapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View promptView = inflater.inflate(R.layout.materialhunter_custom_dialog_view, null);
        final TextView titleTextView = promptView.findViewById(R.id.f_materialhunter_adb_tv_title1);
        final EditText storedpathEditText = promptView.findViewById(R.id.f_materialhunter_adb_et_storedpath);
        switch (item.getItemId()) {
            case R.id.f_materialhunter_menu_backupDB:
                titleTextView.setText("Full path to where you want to save the database:");
                storedpathEditText.setText(NhPaths.APP_SD_SQLBACKUP_PATH + "/FragmentMaterialHunter");
                AlertDialog.Builder adbBackup = new AlertDialog.Builder(activity);
                adbBackup.setView(promptView);
                adbBackup.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
                adbBackup.setPositiveButton("OK", (dialog, which) -> {
                });
                final AlertDialog adBackup = adbBackup.create();
                adBackup.setOnShowListener(dialog -> {
                    final Button buttonOK = adBackup.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult = MaterialHunterData.getInstance().backupData(MaterialHunterSQL.getInstance(context), storedpathEditText.getText().toString());
                        if (returnedResult == null) {
                            NhPaths.showSnack(getView(), "db is successfully backup to " + storedpathEditText.getText().toString(), 1);
                        } else {
                            dialog.dismiss();
                            new AlertDialog.Builder(context).setTitle("Failed to backup the DB.").setMessage(returnedResult).create().show();
                        }
                        dialog.dismiss();
                    });
                });
                adBackup.show();
                break;
            case R.id.f_materialhunter_menu_restoreDB:
                titleTextView.setText("Full path of the db file from where you want to restore:");
                storedpathEditText.setText(NhPaths.APP_SD_SQLBACKUP_PATH + "/FragmentMaterialHunter");
                AlertDialog.Builder adbRestore = new AlertDialog.Builder(activity);
                adbRestore.setView(promptView);
                adbRestore.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
                adbRestore.setPositiveButton("OK", (dialog, which) -> {
                });
                final AlertDialog adRestore = adbRestore.create();
                adRestore.setOnShowListener(dialog -> {
                    final Button buttonOK = adRestore.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult = MaterialHunterData.getInstance().restoreData(MaterialHunterSQL.getInstance(context), storedpathEditText.getText().toString());
                        if (returnedResult == null) {
                            NhPaths.showSnack(getView(), "db is successfully restored to " + storedpathEditText.getText().toString(), 1);
                        } else {
                            dialog.dismiss();
                            new AlertDialog.Builder(context).setTitle("Failed to restore the DB.").setMessage(returnedResult).create().show();
                        }
                        dialog.dismiss();
                    });
                });
                adRestore.show();
                break;
            case R.id.f_materialhunter_menu_ResetToDefault:
                MaterialHunterData.getInstance().resetData(MaterialHunterSQL.getInstance(context));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        MaterialHunterData.getInstance().refreshData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        addButton = null;
        deleteButton = null;
        moveButton = null;
        materialhunterRecyclerViewAdapter = null;
    }

    private void onAddItemSetup() {
        addButton.setOnClickListener(v -> {
            List<MaterialHunterModel> materialhunterModelList = MaterialHunterData.getInstance().materialhunterModelListFull;
            if (materialhunterModelList == null) return;
            final LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewAdd = mInflater.inflate(R.layout.materialhunter_add_dialog_view, null);
            final EditText titleEditText = promptViewAdd.findViewById(R.id.f_materialhunter_add_adb_et_title);
            final EditText cmdEditText = promptViewAdd.findViewById(R.id.f_materialhunter_add_adb_et_command);
            final EditText delimiterEditText = promptViewAdd.findViewById(R.id.f_materialhunter_add_adb_et_delimiter);
            final CheckBox runOnCreateCheckbox = promptViewAdd.findViewById(R.id.f_materialhunters_add_adb_checkbox_runoncreate);
            final Spinner insertPositions = promptViewAdd.findViewById(R.id.f_materialhunter_add_adb_spr_positions);
            final Spinner insertTitles = promptViewAdd.findViewById(R.id.f_materialhunter_add_adb_spr_titles);
            ArrayList<String> titleArrayList = new ArrayList<>();
            for (MaterialHunterModel materialhunterModel : materialhunterModelList) {
                titleArrayList.add(materialhunterModel.getTitle());
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, titleArrayList);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            final FloatingActionButton readmeButton1 = promptViewAdd.findViewById(R.id.f_materialhunter_add_btn_info_fab1);
            final FloatingActionButton readmeButton2 = promptViewAdd.findViewById(R.id.f_materialhunter_add_btn_info_fab2);
            final FloatingActionButton readmeButton3 = promptViewAdd.findViewById(R.id.f_materialhunter_add_btn_info_fab3);

            readmeButton1.setOnClickListener(view -> {
                AlertDialog.Builder adb = new AlertDialog.Builder(context);
                adb.setTitle("HOW TO USE:")
                        .setMessage(context.getString(R.string.materialhunter_howtouse_cmd))
                        .setNegativeButton("Close", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            readmeButton2.setOnClickListener(view -> {
                AlertDialog.Builder adb = new AlertDialog.Builder(context);
                adb.setTitle("HOW TO USE:")
                        .setMessage(context.getString(R.string.materialhunter_howtouse_delimiter))
                        .setNegativeButton("Close", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            readmeButton3.setOnClickListener(view -> {
                AlertDialog.Builder adb = new AlertDialog.Builder(context);
                adb.setTitle("HOW TO USE:")
                        .setMessage(context.getString(R.string.materialhunter_howtouse_runoncreate))
                        .setNegativeButton("Close", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            delimiterEditText.setText("\\n");
            runOnCreateCheckbox.setChecked(true);

            insertPositions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    //if Insert to Top
                    if (position == 0) {
                        insertTitles.setVisibility(View.INVISIBLE);
                        targetPositionId = 1;
                        //if Insert to Bottom
                    } else if (position == 1) {
                        insertTitles.setVisibility(View.INVISIBLE);
                        targetPositionId = materialhunterModelList.size() + 1;
                        //if Insert Before
                    } else if (position == 2) {
                        insertTitles.setVisibility(View.VISIBLE);
                        insertTitles.setAdapter(arrayAdapter);
                        insertTitles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                targetPositionId = position + 1;
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
                        //if Insert After
                    } else {
                        insertTitles.setVisibility(View.VISIBLE);
                        insertTitles.setAdapter(arrayAdapter);
                        insertTitles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                targetPositionId = position + 2;
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            AlertDialog.Builder adb = new AlertDialog.Builder(context);
            adb.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
            adb.setPositiveButton("OK", (dialog, which) -> {
            });
            final AlertDialog ad = adb.create();
            ad.setView(promptViewAdd);
            ad.setCancelable(true);
            ad.setOnShowListener(dialog -> {
                final Button buttonAdd = ad.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonAdd.setOnClickListener(v1 -> {
                    if (titleEditText.getText().toString().isEmpty()) {
                        NhPaths.showMessage_long(context, "Title cannot be empty");
                    } else if (cmdEditText.getText().toString().isEmpty()) {
                        NhPaths.showMessage_long(context, "Command cannot be empty");
                    } else if (delimiterEditText.getText().toString().isEmpty()) {
                        NhPaths.showMessage_long(context, "Delimiter cannot be empty");
                    } else {
                        ArrayList<String> dataArrayList = new ArrayList<>();
                        dataArrayList.add(titleEditText.getText().toString());
                        dataArrayList.add(cmdEditText.getText().toString());
                        dataArrayList.add(delimiterEditText.getText().toString());
                        dataArrayList.add(runOnCreateCheckbox.isChecked() ? "1" : "0");
                        MaterialHunterData.getInstance().addData(targetPositionId, dataArrayList, MaterialHunterSQL.getInstance(context));
                        ad.dismiss();
                    }
                });
            });
            ad.show();
        });
    }

    private void onDeleteItemSetup() {
        deleteButton.setOnClickListener(v -> {
            List<MaterialHunterModel> materialhunterModelList = MaterialHunterData.getInstance().materialhunterModelListFull;
            if (materialhunterModelList == null) return;
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewDelete = inflater.inflate(R.layout.materialhunter_delete_dialog_view, null, false);
            final RecyclerView recyclerViewDeleteItem = promptViewDelete.findViewById(R.id.f_materialhunter_delete_recyclerview);
            MaterialHunterRecyclerViewAdapterDeleteItems materialhunterRecyclerViewAdapterDeleteItems = new MaterialHunterRecyclerViewAdapterDeleteItems(context, materialhunterModelList);
            LinearLayoutManager linearLayoutManagerDelete = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            recyclerViewDeleteItem.setLayoutManager(linearLayoutManagerDelete);
            recyclerViewDeleteItem.setAdapter(materialhunterRecyclerViewAdapterDeleteItems);

            AlertDialog.Builder adbDelete = new AlertDialog.Builder(activity);
            adbDelete.setView(promptViewDelete);
            adbDelete.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
            adbDelete.setPositiveButton("Delete", (dialog, which) -> {
            });
            //If you want the dialog to stay open after clicking OK, you need to do it this way...
            final AlertDialog adDelete = adbDelete.create();
            adDelete.setMessage("Select the item you want to remove: ");
            adDelete.setOnShowListener(dialog -> {
                final Button buttonDelete = adDelete.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonDelete.setOnClickListener(v1 -> {
                    RecyclerView.ViewHolder viewHolder;
                    ArrayList<Integer> selectedPosition = new ArrayList<>();
                    ArrayList<Integer> selectedTargetIds = new ArrayList<>();
                    for (int i = 0; i < recyclerViewDeleteItem.getChildCount(); i++) {
                        viewHolder = recyclerViewDeleteItem.findViewHolderForAdapterPosition(i);
                        if (viewHolder != null) {
                            CheckBox box = viewHolder.itemView.findViewById(R.id.f_materialhunter_recyclerview_dialog_chkbox);
                            if (box.isChecked()) {
                                selectedPosition.add(i);
                                selectedTargetIds.add(i + 1);
                            }
                        }
                    }
                    if (selectedPosition.size() != 0) {
                        MaterialHunterData.getInstance().deleteData(selectedPosition, selectedTargetIds, MaterialHunterSQL.getInstance(context));
                        NhPaths.showSnack(getView(), "Successfully deleted " + selectedPosition.size() + " items.", 1);
                        adDelete.dismiss();
                    } else NhPaths.showMessage_long(context, "Nothing to be deleted");
                });
            });
            adDelete.show();
        });
    }

    private void onMoveItemSetup() {
        moveButton.setOnClickListener(v -> {
            List<MaterialHunterModel> materialhunterModelList = MaterialHunterData.getInstance().materialhunterModelListFull;
            if (materialhunterModelList == null) return;
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewMove = inflater.inflate(R.layout.materialhunter_move_dialog_view, null, false);
            final Spinner titlesBefore = promptViewMove.findViewById(R.id.f_materialhunter_move_adb_spr_titlesbefore);
            final Spinner titlesAfter = promptViewMove.findViewById(R.id.f_materialhunter_move_adb_spr_titlesafter);
            final Spinner actions = promptViewMove.findViewById(R.id.f_materialhunter_move_adb_spr_actions);
            ArrayList<String> titleArrayList = new ArrayList<>();
            for (MaterialHunterModel materialhunterModel : materialhunterModelList) {
                titleArrayList.add(materialhunterModel.getTitle());
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, titleArrayList);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            titlesBefore.setAdapter(arrayAdapter);
            titlesAfter.setAdapter(arrayAdapter);

            AlertDialog.Builder adbMove = new AlertDialog.Builder(activity);
            adbMove.setView(promptViewMove);
            adbMove.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
            adbMove.setPositiveButton("Move", (dialog, which) -> {

            });
            final AlertDialog adMove = adbMove.create();
            adMove.setOnShowListener(dialog -> {
                final Button buttonMove = adMove.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonMove.setOnClickListener(v1 -> {
                    int originalPositionIndex = titlesBefore.getSelectedItemPosition();
                    int targetPositionIndex = titlesAfter.getSelectedItemPosition();
                    if (originalPositionIndex == targetPositionIndex ||
                            (actions.getSelectedItemPosition() == 0 && targetPositionIndex == (originalPositionIndex + 1)) ||
                            (actions.getSelectedItemPosition() == 1 && targetPositionIndex == (originalPositionIndex - 1))) {
                        NhPaths.showMessage(context, "You are moving the item to the same position, nothing to be moved.");
                    } else {
                        if (actions.getSelectedItemPosition() == 1) targetPositionIndex += 1;
                        MaterialHunterData.getInstance().moveData(originalPositionIndex, targetPositionIndex, MaterialHunterSQL.getInstance(context));
                        NhPaths.showSnack(getView(), "Successfully moved item.", 1);
                        adMove.dismiss();
                    }
                });
            });
            adMove.show();
        });
    }
}
