package com.alorma.github.ui.fragment.donate;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.alorma.github.R;
import com.alorma.github.ui.adapter.base.RecyclerArrayAdapter;
import com.alorma.github.ui.fragment.base.BaseFragment;
import com.alorma.github.ui.utils.DialogUtils;
import com.android.vending.billing.IInAppBillingService;
import java.util.ArrayList;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

public class DonateFragment extends BaseFragment {

  private static final String SKU_BASE_DONATE = "com.alorma.github.donate";
  public ArrayList<DonateItem> skuList;
  private String purchaseId;
  private IInAppBillingService mService;
  ServiceConnection mServiceConn = new ServiceConnection() {

    @Override
    public void onServiceDisconnected(ComponentName name) {
      mService = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      mService = IInAppBillingService.Stub.asInterface(service);
    }
  };

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    skuList = new ArrayList<>();
    skuList.add(new DonateItem(SKU_BASE_DONATE + ".smallest", 1));
    skuList.add(new DonateItem(SKU_BASE_DONATE + ".small", 2));
    skuList.add(new DonateItem(SKU_BASE_DONATE, 5));
    skuList.add(new DonateItem(SKU_BASE_DONATE + ".big", 10));
    skuList.add(new DonateItem(SKU_BASE_DONATE + ".awesome", 20));

    createBillingService();
  }

  private void createBillingService() {
    Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
    serviceIntent.setPackage("com.android.vending");
    getActivity().bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
  }

  public void launchDonate() {
    DonateItemsAdapter adapter = new DonateItemsAdapter(LayoutInflater.from(getActivity()));
    adapter.addAll(skuList);
    adapter.setCallback(item -> {
      if (dialog != null) {
        dialog.dismiss();
      }
      buy(item.getSku());
    });
    dialog = new DialogUtils().builder(getActivity())
        .title(R.string.support_development)
        .adapter(adapter, new LinearLayoutManager(getActivity()))
        .show();
  }

  private void buy(String sku) {
    try {
      if (mService != null) {
        purchaseId = UUID.randomUUID().toString();
        Bundle buyIntentBundle = mService.getBuyIntent(3, getActivity().getPackageName(), sku, "inapp", purchaseId);
        if (buyIntentBundle != null) {
          PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
          if (pendingIntent != null) {
            getActivity().startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);
          }
        }
      }
    } catch (RemoteException | IntentSender.SendIntentException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == 1001) {
      String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

      if (resultCode == Activity.RESULT_OK) {
        try {
          JSONObject jo = new JSONObject(purchaseData);
          String sku = jo.getString("productId");
          String developerPayload = jo.getString("developerPayload");
          if (developerPayload.equals(purchaseId) && SKU_BASE_DONATE.equals(sku)) {
            giveThanksForBuyDonate();
          }
        } catch (JSONException e) {

          e.printStackTrace();
        }
      }
    }
  }

  private void giveThanksForBuyDonate() {
    Toast.makeText(getActivity(), getString(R.string.thanks_for_donate), Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onDestroy() {
    if (mService != null) {
      getActivity().unbindService(mServiceConn);
    }
    super.onDestroy();
  }

  public boolean enabled() {
    return true;
  }

  public class DonateItemsAdapter extends RecyclerArrayAdapter<DonateItem, DonateItemsAdapter.Holder> {

    public DonateItemsAdapter(LayoutInflater inflater) {
      super(inflater);
    }

    @Override
    protected void onBindViewHolder(Holder holder, DonateItem donateItem) {
      holder.textView.setText(donateItem.toString());
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new Holder(getInflater().inflate(android.R.layout.simple_list_item_1, parent, false));
    }

    public class Holder extends RecyclerView.ViewHolder {
      @BindView(android.R.id.text1) TextView textView;
      public Holder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
      }
    }
  }
}
