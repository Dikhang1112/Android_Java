package com.voggella.android.doan.mainHome;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.voggella.android.doan.Database_Adapter.MultiTypeAdapter;
import com.voggella.android.doan.Database_Adapter.SQLiteHelper;
import com.voggella.android.doan.R;
import com.voggella.android.doan.Database_Adapter.Transaction; // Import Transaction
import com.voggella.android.doan.Database_Adapter.Budget; // Import Budget
import java.util.ArrayList;
import java.util.List;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;

public class mainScreen extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MultiTypeAdapter multiTypeAdapter;
    private List<Object> itemList; // Danh sách chứa cả Transaction và Budget
    private SQLiteHelper dbHelper;
    private String phoneUser;
    private String userName;
    private ActivityResultLauncher<Intent> addTransactionLauncher;
    private TextView textNotificationCount;
    private ImageView imageNoti;
    private int notificationCount = 0; //Bien dem thong bao
    private Switch switchDarkMode;
    private SharedPreferences sharedPreferences;
    private boolean isVipUser = false;
    private FooterLayout footerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

        // Khởi tạo database trước để có thể kiểm tra VIP
        dbHelper = new SQLiteHelper(this);
        // Lấy thông tin user và kiểm tra VIP
        phoneUser = getIntent().getStringExtra("USERS_SDT");
        userName = getIntent().getStringExtra("USER_FULL_NAME");


        //Khoi tao ui cho switch
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchDarkMode.setChecked(AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);

        // Lắng nghe sự kiện thay đổi của Switch
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Chuyển sang chế độ tối
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                // Chuyển sang chế độ sáng
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Khởi tạo ImageView và TextView
        footerLayout = findViewById(R.id.footerLayout);
        textNotificationCount = findViewById(R.id.textNotificationCount);
        imageNoti = findViewById(R.id.imageNoti);

        //Lay sdt va ho ten cho footer
        footerLayout.setUserData(phoneUser,userName);

        // Kiểm tra quyền VIP ngay sau khi lấy được phoneUser
        phoneUser = getIntent().getStringExtra("USERS_SDT");
        if (phoneUser != null) {
            checkVipStatus();
        } else {
            Log.e("MainScreen", "phoneUser is null");
        }
        imageNoti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNotificationDialog();
                notificationCount = 0;
                textNotificationCount.setText(String.valueOf(notificationCount));
            }
        });

        // Nhận thông tin người dùng từ Intent
        userName = getIntent().getStringExtra("USER_FULL_NAME");
        phoneUser = getIntent().getStringExtra("USERS_SDT");
        if (phoneUser == null || phoneUser.isEmpty()) {
            Log.e("mainScreen", "Số điện thoại không hợp lệ.");
            Toast.makeText(this, "Không nhận được thông tin người dùng!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        addTransactionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        refreshData(); // Gọi refreshData để cập nhật RecyclerView
                        Toast.makeText(this, "Giao dịch đã được thêm.", Toast.LENGTH_SHORT).show();
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Toast.makeText(this, "Đã hủy thêm giao dịch", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Lấy TextView name
        TextView viewName = findViewById(R.id.ViewName);
        String fullName = getUserFullName(phoneUser);
        if (fullName != null) {
            viewName.setText(fullName);
        } else {
            viewName.setText("Người dùng không xác định");
        }

        // Setup RecyclerView
        recyclerView = findViewById(R.id.viewTrans);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load dữ liệu
        loadData(phoneUser);

        // Setup TextView hover effect
        TextView viewBudget = findViewById(R.id.ViewBudget);
        viewBudget.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    viewBudget.setBackgroundColor(Color.parseColor("#D3D3D3"));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    viewBudget.setBackgroundColor(Color.TRANSPARENT);
                    break;
            }
            return false;
        });

        // Hiển thị Dialog khi ấn vào TextView
        viewBudget.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_budget, null);
            builder.setView(dialogView);

            EditText edtAmount = dialogView.findViewById(R.id.edtAmount);
            EditText edtDate = dialogView.findViewById(R.id.edtDate);
            Button btnSave = dialogView.findViewById(R.id.btnSave);
            AlertDialog dialog = builder.create();
            dialog.show();

            btnSave.setOnClickListener(saveView -> {
                String amount = edtAmount.getText().toString().trim();
                String date = edtDate.getText().toString().trim();
                if (amount.isEmpty() || date.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                    return;
                }

                dbHelper.addBudget(phoneUser, Double.parseDouble(amount), date);
                String balanceText = "Số dư: " + amount + " vnd";
                viewBudget.setText(balanceText);
                Toast.makeText(this, "Lưu thông tin thành công!", Toast.LENGTH_SHORT).show();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                loadData(phoneUser); // Cập nhật lại RecyclerView
                recreate();
            });
        });

        // Kiểm tra quyền VIP
        checkVipStatus();
    }

    private void checkVipStatus() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Log.d("MainScreen", "Checking VIP status for user: " + phoneUser);

        try {
            Cursor cursor = db.query(
                    SQLiteHelper.TB_USERS,
                    new String[]{SQLiteHelper.COLUMN_USER_TYPE},
                    SQLiteHelper.COLUMN_USER_SDT + "=?",
                    new String[]{phoneUser},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range")
                int vipStatus = cursor.getInt(cursor.getColumnIndex(SQLiteHelper.COLUMN_USER_TYPE));
                isVipUser = (vipStatus == 1);
                Log.d("MainScreen", "User VIP status: " + isVipUser);

                runOnUiThread(() -> {
                    if (isVipUser) {
                        switchDarkMode.setVisibility(View.VISIBLE);
                    } else {
                        switchDarkMode.setVisibility(View.GONE);
                        // Đảm bảo user thường luôn ở light mode
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    }
                });

                cursor.close();
            }
        } catch (Exception e) {
            Log.e("MainScreen", "Error checking VIP status: " + e.getMessage());
        }
    }



    private void refreshData() {
        if (itemList != null && multiTypeAdapter != null) {
            itemList.clear();
            loadData(phoneUser);
            multiTypeAdapter.notifyDataSetChanged();
        }
    }
    // Phương thức load dữ liệu cả Transaction và Budget
    private void loadData(String phoneUser) {
        itemList = new ArrayList<>();
        // Load Transaction data
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] transactionColumns = {
                SQLiteHelper.COLUMN_TRANSACTION_TYPE,
                SQLiteHelper.COLUMN_TRANSACTION_AMOUNT,
                SQLiteHelper.COLUMN_TRANSACTION_DATE
        };
        // Thêm điều kiện Transaction Amount khác 0
        String transactionSelection = SQLiteHelper.COLUMN_TRANSACTION_USERS_SDT + " = ? AND " +
                SQLiteHelper.COLUMN_TRANSACTION_ID + " != ? AND " +
                SQLiteHelper.COLUMN_TRANSACTION_AMOUNT + " != 0";
        Cursor transactionCursor = db.query(SQLiteHelper.TB_Trans, transactionColumns, transactionSelection, new String[]{phoneUser, "1"}, null, null, null);

        if (transactionCursor != null) {
            while (transactionCursor.moveToNext()) {
                @SuppressLint("Range") String type = transactionCursor.getString(transactionCursor.getColumnIndex(SQLiteHelper.COLUMN_TRANSACTION_TYPE));
                @SuppressLint("Range") double amount = transactionCursor.getDouble(transactionCursor.getColumnIndex(SQLiteHelper.COLUMN_TRANSACTION_AMOUNT));
                @SuppressLint("Range") String date = transactionCursor.getString(transactionCursor.getColumnIndex(SQLiteHelper.COLUMN_TRANSACTION_DATE));

                itemList.add(new Transaction(type, amount, date));
                notificationCount++; // Tăng số đếm
                textNotificationCount.setText(String.valueOf(notificationCount));
            }
            transactionCursor.close();
        }

        // Load Budget data
        String[] budgetColumns = {
                SQLiteHelper.COLUMN_BUDGET_DATA,
                SQLiteHelper.COLUMN_BUDGET_DATE
        };
        // Thêm điều kiện Budget Data khác 0
        String budgetSelection = SQLiteHelper.COLUMN_BUDGET_USERS_SDT + " = ? AND " +
                SQLiteHelper.COLUMN_BUDGET_ID + " != ? AND " +
                SQLiteHelper.COLUMN_BUDGET_DATA + " != 0";
        Cursor budgetCursor = db.query(SQLiteHelper.TB_Budget, budgetColumns, budgetSelection, new String[]{phoneUser, "1"}, null, null, null);

        if (budgetCursor != null) {
            while (budgetCursor.moveToNext()) {
                @SuppressLint("Range") double amount = budgetCursor.getDouble(budgetCursor.getColumnIndex(SQLiteHelper.COLUMN_BUDGET_DATA));
                @SuppressLint("Range") String date = budgetCursor.getString(budgetCursor.getColumnIndex(SQLiteHelper.COLUMN_BUDGET_DATE));

                itemList.add(new Budget(amount, date));
                notificationCount++; // Tăng số đếm
                textNotificationCount.setText(String.valueOf(notificationCount));
            }
            budgetCursor.close();
        }

        db.close();

        // Cập nhật RecyclerView với adapter MultiTypeAdapter
        if (multiTypeAdapter == null) {
            multiTypeAdapter = new MultiTypeAdapter(itemList);
            recyclerView.setAdapter(multiTypeAdapter);
        } else {
            multiTypeAdapter.notifyDataSetChanged();
        }
        // Cập nhật lại ViewBudget
        updateViewBudget(phoneUser);
    }

    // Phương thức cập nhật ViewBudget
    private void updateViewBudget(String phoneUser) {
        double totalBudget = getTotalBudget(phoneUser); // Tổng tiền trong bảng Budget
        double totalTransaction = getTotalTransaction(phoneUser); // Tổng tiền trong bảng Transaction

        // Cập nhật ViewBudget với giá trị tổng ngân sách - tổng giao dịch
        TextView viewBudget = findViewById(R.id.ViewBudget);
        double remainingBalance = totalBudget - totalTransaction;

        // Hiển thị số dư còn lại
        String balanceText = "Số dư: " + remainingBalance + " vnd";
        viewBudget.setText(balanceText);
    }

    // Lấy tổng tiền trong bảng Budget
    private double getTotalBudget(String phoneUser) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double total = 0;
        Cursor cursor = null;

        try {
            String query = "SELECT SUM(" + SQLiteHelper.COLUMN_BUDGET_DATA + ") FROM " + SQLiteHelper.TB_Budget + " WHERE " + SQLiteHelper.COLUMN_BUDGET_USERS_SDT + " = ?";
            cursor = db.rawQuery(query, new String[]{phoneUser});

            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e("mainScreen", "Lỗi khi tính tổng ngân sách", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return total;
    }

    // Lấy tổng tiền trong bảng Transaction
    private double getTotalTransaction(String phoneUser) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        double total = 0;
        Cursor cursor = null;

        try {
            String query = "SELECT SUM(" + SQLiteHelper.COLUMN_TRANSACTION_AMOUNT + ") FROM " + SQLiteHelper.TB_Trans + " WHERE " + SQLiteHelper.COLUMN_TRANSACTION_USERS_SDT + " = ?";
            cursor = db.rawQuery(query, new String[]{phoneUser});

            if (cursor != null && cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e("mainScreen", "Lỗi khi tính tổng giao dịch", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return total;
    }

    // Lấy tên đầy đủ của người dùng
    @SuppressLint("Range")
    private String getUserFullName(String phoneUser) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT " + SQLiteHelper.COLUMN_USER_NAME + " FROM " + SQLiteHelper.TB_USERS + " WHERE " + SQLiteHelper.COLUMN_USER_SDT + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{phoneUser});
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_USER_NAME));
        }
        return null;
    }
    // Phương thức hiển thị Dialog khi nhấn vào ImageView
    private void showNotificationDialog() {
        // Tạo một thông báo AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thông báo");
        String notice = "Bạn vừa cập nhật " + notificationCount +" giao dịch";
        builder.setMessage(notice);
        builder.setCancelable(false);  // Không thể thoát bằng cách nhấn ngoài

        // Thêm nút "OK" để đóng Dialog
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Đóng Dialog khi người dùng nhấn OK
                dialog.dismiss();
            }
        });

        // Hiển thị Dialog
        builder.create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isVipUser) {
            // Đồng bộ trạng thái switch với theme hiện tại
            boolean isDarkMode = getDarkModeState();
            switchDarkMode.setChecked(isDarkMode);
        }
    }

    // Thêm phương thức này để xử lý việc chuyển đổi theme
    @Override
    public void onNightModeChanged(int mode) {
        super.onNightModeChanged(mode);
        recreate();
    }

    // Thêm phương thức kiểm tra VIP
    private boolean checkIsUserVip(String phone) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.query(
                    SQLiteHelper.TB_USERS,
                    new String[]{SQLiteHelper.COLUMN_USER_TYPE},
                    SQLiteHelper.COLUMN_USER_SDT + "=?",
                    new String[]{phone},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range")
                int vipStatus = cursor.getInt(cursor.getColumnIndex(SQLiteHelper.COLUMN_USER_TYPE));
                cursor.close();
                return vipStatus == 1;
            }
        } catch (Exception e) {
            Log.e("MainScreen", "Error checking VIP status: " + e.getMessage());
        }
        return false;
    }
    private boolean getDarkModeState() {
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        return sharedPreferences.getBoolean("dark_mode", false);
    }

    // Hàm lưu trạng thái Dark Mode vào SharedPreferences
    private void saveDarkModeState(boolean isDarkMode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("dark_mode", isDarkMode);
        editor.apply();
    }

}