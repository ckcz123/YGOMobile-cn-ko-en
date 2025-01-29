package cn.garymb.ygomobile.ui.cards;


import android.content.Context;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView.OnEditorActionListener;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.garymb.ygomobile.AppsSettings;
import cn.garymb.ygomobile.lite.R;
import cn.garymb.ygomobile.loader.CardSearchInfo;
import cn.garymb.ygomobile.loader.ICardSearcher;
import cn.garymb.ygomobile.ui.adapters.SimpleSpinnerAdapter;
import cn.garymb.ygomobile.ui.adapters.SimpleSpinnerItem;
import cn.garymb.ygomobile.ui.plus.DialogPlus;
import cn.garymb.ygomobile.ui.plus.VUiKit;
import ocgcore.CardManager;
import ocgcore.DataManager;
import ocgcore.LimitManager;
import ocgcore.StringManager;
import ocgcore.data.Card;
import ocgcore.data.CardSet;
import ocgcore.data.LimitList;
import ocgcore.enums.CardAttribute;
import ocgcore.enums.CardCategory;
import ocgcore.enums.CardOt;
import ocgcore.enums.CardRace;
import ocgcore.enums.CardType;
import ocgcore.enums.LimitType;

public class CardSearcher implements View.OnClickListener {
    private static final String TAG = "CardSearcher";
    final String[] BtnVals = new String[9];
    private final EditText keyWord;
    private final CheckBox chk_multi_keyword;
    private final Spinner otSpinner;
    private final Spinner limitSpinner;
    private final Spinner limitListSpinner;
    private final Spinner typeSpinner;
    private final Spinner typeMonsterSpinner;
    private final Spinner typeMonsterSpinner2;
    private final Spinner typeSpellSpinner;
    private final Spinner typeTrapSpinner;
    private final Spinner setCodeSpinner;
    private final Spinner categorySpinner;
    private final Spinner raceSpinner;
    private final Spinner levelSpinner;
    private final Spinner attributeSpinner;
    private final EditText atkText;
    private final EditText defText;
    private final Spinner pScale;
    private final Button LinkMarkerButton;
    private final Button searchButton;
    private final Button resetButton;
    private final View view;
    private final View layout_monster;
    private final ICardSearcher mICardSearcher;
    private final Context mContext;
    private final Button myFavButton;
    protected StringManager mStringManager;
    protected LimitManager mLimitManager;
    protected AppsSettings mSettings;
    private int lineKey;
    private CallBack mCallBack;
    private boolean mShowFavorite;

    public CardSearcher(View view, ICardSearcher iCardSearcher) {
        this.view = view;
        this.mContext = view.getContext();
        this.mICardSearcher = iCardSearcher;
        this.mSettings = AppsSettings.get();
        mStringManager = DataManager.get().getStringManager();
        mLimitManager = DataManager.get().getLimitManager();
        keyWord = findViewById(R.id.edt_word1);
        chk_multi_keyword = findViewById(R.id.chk_multi_keyword);
        otSpinner = findViewById(R.id.sp_ot);
        limitSpinner = findViewById(R.id.sp_limit);
        limitListSpinner = findViewById(R.id.sp_limit_list);
        typeSpinner = findViewById(R.id.sp_type_card);
        typeMonsterSpinner = findViewById(R.id.sp_type_monster);
        typeMonsterSpinner2 = findViewById(R.id.sp_type_monster2);
        typeSpellSpinner = findViewById(R.id.sp_type_spell);
        typeTrapSpinner = findViewById(R.id.sp_type_trap);
        setCodeSpinner = findViewById(R.id.sp_setcode);
        categorySpinner = findViewById(R.id.sp_category);
        raceSpinner = findViewById(R.id.sp_race);
        levelSpinner = findViewById(R.id.sp_level);
        attributeSpinner = findViewById(R.id.sp_attribute);
        atkText = findViewById(R.id.edt_atk);
        defText = findViewById(R.id.edt_def);
        LinkMarkerButton = findViewById(R.id.btn_linkmarker);
        myFavButton = findViewById(R.id.btn_my_fav);
        searchButton = findViewById(R.id.btn_search);
        resetButton = findViewById(R.id.btn_reset);
        layout_monster = findViewById(R.id.layout_monster);
        pScale = findViewById(R.id.sp_scale);
        myFavButton.setOnClickListener(this);
        LinkMarkerButton.setOnClickListener(this);
        searchButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);

        //输入即时搜索
        OnEditorActionListener searchListener = (v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                search();
                return true;
            }
            return false;
        };

        keyWord.setOnEditorActionListener(searchListener);
        chk_multi_keyword.setChecked(mSettings.getKeyWordsSplit() == 0 ? false : true);
        chk_multi_keyword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings.setKeyWordsSplit(isChecked ? 1 : 0);
            }
        });

        myFavButton.setOnClickListener(v -> {
            if (isShowFavorite()) {
                hideFavorites(true);
            } else {
                showFavorites(true);
            }
            // initSetCode();
        });

        LinkMarkerButton.setOnClickListener(v -> {
            Arrays.fill(BtnVals, "0");
            DialogPlus viewDialog = new DialogPlus(mContext);
            viewDialog.setContentView(R.layout.item_linkmarker);
            viewDialog.setTitle(R.string.ClickLinkArrows);
            viewDialog.show();
            int[] ids = new int[]{
                    R.id.button_1,
                    R.id.button_2,
                    R.id.button_3,
                    R.id.button_4,
                    R.id.button_5,
                    R.id.button_6,
                    R.id.button_7,
                    R.id.button_8,
                    R.id.button_9,
            };
            int[] enImgs = new int[]{
                    R.drawable.left_bottom_1,
                    R.drawable.bottom_1,
                    R.drawable.right_bottom_1,
                    R.drawable.left_1,
                    0,
                    R.drawable.right_1,
                    R.drawable.left_top_1,
                    R.drawable.top_1,
                    R.drawable.right_top_1,
            };
            int[] disImgs = new int[]{
                    R.drawable.left_bottom_0,
                    R.drawable.bottom_0,
                    R.drawable.right_bottom_0,
                    R.drawable.left_0,
                    0,
                    R.drawable.right_0,
                    R.drawable.left_top_0,
                    R.drawable.top_0,
                    R.drawable.right_top_0,
            };
            for (int i = 0; i < ids.length; i++) {
                final int index = i;
                viewDialog.findViewById(ids[index]).setOnClickListener((btn) -> {
                    if (index == 4) {
                        String mLinkStr = BtnVals[8] + BtnVals[7] + BtnVals[6] + BtnVals[5] + "0"
                                + BtnVals[3] + BtnVals[2] + BtnVals[1] + BtnVals[0];
                        lineKey = Integer.parseInt(mLinkStr, 2);
                        if (viewDialog.isShowing()) {
                            viewDialog.dismiss();
                        }
                    } else {
                        if ("0".equals(BtnVals[index])) {
                            btn.setBackgroundResource(enImgs[index]);
                            BtnVals[index] = "1";
                        } else {
                            btn.setBackgroundResource(disImgs[index]);
                            BtnVals[index] = "0";
                        }
                    }
                });
            }
        });


        limitListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                long value = getSelect(limitListSpinner);
                if (value <= 0) {
                    reset(limitSpinner);
                    limitSpinner.setVisibility(View.INVISIBLE);
                } else {
                    limitSpinner.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                long value = getSelect(typeSpinner);
                if (value == 0) {
                    layout_monster.setVisibility(View.INVISIBLE);
                    raceSpinner.setVisibility(View.GONE);
                    typeSpellSpinner.setVisibility(View.GONE);
                    typeTrapSpinner.setVisibility(View.GONE);
                    pScale.setVisibility(View.INVISIBLE);
                    LinkMarkerButton.setVisibility(View.INVISIBLE);
                    resetMonster();
                } else if (value == CardType.Spell.getId()) {
                    layout_monster.setVisibility(View.INVISIBLE);
                    raceSpinner.setVisibility(View.GONE);
                    typeSpellSpinner.setVisibility(View.VISIBLE);
                    typeTrapSpinner.setVisibility(View.GONE);
                    pScale.setVisibility(View.INVISIBLE);
                    LinkMarkerButton.setVisibility(View.INVISIBLE);
                    resetMonster();
                } else if (value == CardType.Trap.getId()) {
                    layout_monster.setVisibility(View.INVISIBLE);
                    raceSpinner.setVisibility(View.GONE);
                    typeSpellSpinner.setVisibility(View.GONE);
                    typeTrapSpinner.setVisibility(View.VISIBLE);
                    pScale.setVisibility(View.INVISIBLE);
                    LinkMarkerButton.setVisibility(View.INVISIBLE);
                    resetMonster();
                } else {
                    layout_monster.setVisibility(View.VISIBLE);
                    raceSpinner.setVisibility(View.VISIBLE);
                    typeSpellSpinner.setVisibility(View.GONE);
                    typeTrapSpinner.setVisibility(View.GONE);
                    pScale.setVisibility(View.VISIBLE);
                    LinkMarkerButton.setVisibility(View.VISIBLE);
                }

                reset(pScale);
                reset(raceSpinner);
                reset(typeSpellSpinner);
                reset(typeTrapSpinner);
                reset(typeMonsterSpinner);
                reset(typeMonsterSpinner2);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void setCallBack(CallBack callBack) {
        mCallBack = callBack;
    }

    private void initSetCode() {
        List<CardSet> setnames = mStringManager.getCardSets();
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        boolean first = true;
        SparseArray<Card> cards = DataManager.get().getCardManager().getAllCards();
        for (CardSet cardSet : setnames) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            long code = cardSet.getCode();
            String name = cardSet.getName();
            builder.append("\n  ");
            builder.append("{\"code\": ").append(code);
            builder.append(", \"name\": \"").append(name).append("\"");
            builder.append(", \"data\": [");

            boolean second = true;
            for (int i = 0; i < cards.size(); ++i) {
                Card card = cards.valueAt(i);
                if (card.isSetCode(code)) {
                    if (!second) {
                        builder.append(", ");
                    }
                    second = false;
                    builder.append(card.Code);
                }
            }
            builder.append("]}");
        }
        builder.append("\n]\n");

        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            try {
                Files.write(
                    new File(AppsSettings.get().getResourcePath(), "setcode.json").toPath(),
                    builder.toString().getBytes(StandardCharsets.UTF_8));
                Log.i(TAG, "Successfully write setcode.json!");
            } catch (Exception e) {
                Log.e(TAG, "Unable to write setcode.json!", e);
            }
        }
    }

    public void showFavorites(boolean showList) {
        mShowFavorite = true;
        myFavButton.setSelected(true);
        if (mCallBack != null) {
            mCallBack.onSearchStart();
        }
        if (mCallBack != null) {
            VUiKit.post(() -> {
                mCallBack.onSearchResult(CardFavorites.get().getCards(mICardSearcher), !showList);
            });
        }
    }

    public void hideFavorites(boolean reload) {
        mShowFavorite = false;
        myFavButton.setSelected(false);
        if (mCallBack != null) {
            mCallBack.onSearchStart();
        }
        if (reload) {
            VUiKit.post(() -> {
                search();
            });
        } else {
            if (mCallBack != null) {
                VUiKit.post(() -> {
                    mCallBack.onSearchResult(Collections.emptyList(), true);
                });
            }
        }
    }

    public void initItems() {
        initOtSpinners(otSpinner);
        initLimitSpinners(limitSpinner);
        initLimitListSpinners(limitListSpinner);
        initTypeSpinners(typeSpinner, new CardType[]{CardType.None, CardType.Monster, CardType.Spell, CardType.Trap});
        initTypeSpinners(typeMonsterSpinner, new CardType[]{CardType.None, CardType.Normal, CardType.Effect, CardType.Fusion, CardType.Ritual,
                CardType.Synchro, CardType.Pendulum, CardType.Xyz, CardType.Link, CardType.Spirit, CardType.Union,
                CardType.Dual, CardType.Tuner, CardType.Flip, CardType.Toon, CardType.Sp_Summon, CardType.Token
        });
        initTypeSpinners(typeMonsterSpinner2, new CardType[]{CardType.None, CardType.Pendulum, CardType.Tuner, CardType.Non_Effect
        });
        initTypeSpinners(typeSpellSpinner, new CardType[]{CardType.None, CardType.Normal, CardType.QuickPlay, CardType.Ritual,
                CardType.Continuous, CardType.Equip, CardType.Field
        });
        initTypeSpinners(typeTrapSpinner, new CardType[]{CardType.None, CardType.Normal, CardType.Continuous, CardType.Counter
        });
        initLevelSpinners(levelSpinner);
        initPscaleSpinners(pScale);
        initAttributes(attributeSpinner);
        initRaceSpinners(raceSpinner);
        initSetNameSpinners(setCodeSpinner);
        initCategorySpinners(categorySpinner);
    }

    protected <T extends View> T findViewById(int id) {
        T v = view.findViewById(id);
        if (v instanceof Spinner) {
            ((Spinner) v).setPopupBackgroundResource(R.color.colorNavy);
        }
        return v;
    }

    private void initOtSpinners(Spinner spinner) {
        List<SimpleSpinnerItem> items = new ArrayList<>();
        for (CardOt item : CardOt.values()) {
            items.add(new SimpleSpinnerItem(item.getId(),
                    mStringManager.getOtString(item.getId(), false)));
        }
        SimpleSpinnerAdapter adapter = new SimpleSpinnerAdapter(mContext);
        adapter.setColor(Color.WHITE);
        adapter.setColor(Color.WHITE);
        adapter.set(items);
        spinner.setAdapter(adapter);
    }

    public boolean isShowFavorite() {
        return mShowFavorite;
    }

    protected String getString(int id) {
        return mContext.getString(id);
    }

    private void initLimitSpinners(Spinner spinner) {
        LimitType[] eitems = LimitType.values();
        List<SimpleSpinnerItem> items = new ArrayList<>();
        for (LimitType item : eitems) {
            if (item == LimitType.None) {
                items.add(new SimpleSpinnerItem(item.getId(), getString(R.string.label_limit)));
            } else if (item == LimitType.All) {
                items.add(new SimpleSpinnerItem(item.getId(), getString(R.string.all)));
            } else {
                items.add(new SimpleSpinnerItem(item.getId(), mStringManager.getLimitString(item.getId())));
            }
        }
        SimpleSpinnerAdapter adapter = new SimpleSpinnerAdapter(mContext);
        adapter.setColor(Color.WHITE);
        adapter.set(items);
        spinner.setAdapter(adapter);
    }

    private void initLimitListSpinners(Spinner spinner) {
        List<SimpleSpinnerItem> items = new ArrayList<>();
        List<String> limits = mLimitManager.getLimitNames();
        int index = -1;
        int count = mLimitManager.getCount();
        LimitList cur = null;
        if (mICardSearcher != null) {
            cur = mICardSearcher.getLimitList();
        }
        items.add(new SimpleSpinnerItem(0, getString(R.string.label_limitlist)));
        for (int i = 0; i < count; i++) {
            int j = i + 1;
            String name = limits.get(i);
            items.add(new SimpleSpinnerItem(j, name));
            if (cur != null && TextUtils.equals(cur.getName(), name)) {
                index = j;
            }
        }
        SimpleSpinnerAdapter adapter = new SimpleSpinnerAdapter(mContext);
        adapter.setColor(Color.WHITE);
        adapter.set(items);
        spinner.setAdapter(adapter);
        if (index >= 0) {
            spinner.setSelection(index);
        }
    }

    private void initPscaleSpinners(Spinner spinner) {
        List<SimpleSpinnerItem> items = new ArrayList<>();
        for (int i = -1; i <= 13; i++) {
            if (i == -1) {
                items.add(new SimpleSpinnerItem(i, getString(R.string.label_pendulum)));
            } else {
                items.add(new SimpleSpinnerItem(i, "" + i));
            }
        }
        SimpleSpinnerAdapter adapter = new SimpleSpinnerAdapter(mContext);
        adapter.setColor(Color.WHITE);
        adapter.set(items);
        spinner.setAdapter(adapter);
    }

    private void initLevelSpinners(Spinner spinner) {
        List<SimpleSpinnerItem> items = new ArrayList<>();
        for (int i = 0; i <= 13; i++) {
            if (i == 0) {
                items.add(new SimpleSpinnerItem(i, getString(R.string.label_level)));
            } else {
                items.add(new SimpleSpinnerItem(i, "" + i));
            }
        }
        SimpleSpinnerAdapter adapter = new SimpleSpinnerAdapter(mContext);
        adapter.setColor(Color.WHITE);
        adapter.set(items);
        spinner.setAdapter(adapter);
    }

    private void initSetNameSpinners(Spinner spinner) {
        List<CardSet> setnames = mStringManager.getCardSets();
        List<SimpleSpinnerItem> items = new ArrayList<>();
        items.add(new SimpleSpinnerItem(0, getString(R.string.label_set)));
        for (CardSet set : setnames) {
            items.add(new SimpleSpinnerItem(set.getCode(), set.getName()));
        }
        SimpleSpinnerAdapter adapter = new SimpleSpinnerAdapter(mContext);
        adapter.setColor(Color.WHITE);
        adapter.set(items);
        spinner.setAdapter(adapter);
    }

    private void initTypeSpinners(Spinner spinner, CardType[] eitems) {
        if (eitems == null) {
            eitems = CardType.values();
        }
        List<SimpleSpinnerItem> items = new ArrayList<>();
        for (CardType item : eitems) {
            if (item == CardType.None) {
                items.add(new SimpleSpinnerItem(item.getId(), getString(R.string.label_type)));
            } else {
                items.add(new SimpleSpinnerItem(item.getId(), mStringManager.getTypeString(item.getId())));
            }
        }
        SimpleSpinnerAdapter adapter = new SimpleSpinnerAdapter(mContext);
        adapter.setColor(Color.WHITE);
        adapter.set(items);
        spinner.setAdapter(adapter);
    }

    private void initAttributes(Spinner spinner) {
        CardAttribute[] attributes = CardAttribute.values();
        List<SimpleSpinnerItem> items = new ArrayList<>();
        for (CardAttribute item : attributes) {
            if (item == CardAttribute.None) {
                items.add(new SimpleSpinnerItem(CardAttribute.None.getId(), getString(R.string.label_attr)));
            } else {
                items.add(new SimpleSpinnerItem(item.getId(), mStringManager.getAttributeString(item.getId())));
            }
        }
        SimpleSpinnerAdapter adapter = new SimpleSpinnerAdapter(mContext);
        adapter.setColor(Color.WHITE);
        adapter.set(items);
        spinner.setAdapter(adapter);
    }

    private void initRaceSpinners(Spinner spinner) {
        CardRace[] attributes = CardRace.values();
        List<SimpleSpinnerItem> items = new ArrayList<>();
        for (CardRace item : attributes) {
            long val = item.value();
            if (val == 0) {
                items.add(new SimpleSpinnerItem(val, mContext.getString(R.string.label_race)));
            } else {
                items.add(new SimpleSpinnerItem(val, mStringManager.getRaceString(val)));
            }
        }
        SimpleSpinnerAdapter adapter = new SimpleSpinnerAdapter(mContext);
        adapter.setColor(Color.WHITE);
        adapter.set(items);
        spinner.setAdapter(adapter);
    }

    private void initCategorySpinners(Spinner spinner) {
        CardCategory[] attributes = CardCategory.values();
        List<SimpleSpinnerItem> items = new ArrayList<>();
        for (CardCategory item : attributes) {
            long val = item.value();
            if (val == 0) {
                items.add(new SimpleSpinnerItem(val, mContext.getString(R.string.label_category)));
            } else {
                items.add(new SimpleSpinnerItem(val, mStringManager.getCategoryString(val)));
            }
        }
        SimpleSpinnerAdapter adapter = new SimpleSpinnerAdapter(mContext);
        adapter.setColor(Color.WHITE);
        adapter.set(items);
        spinner.setAdapter(adapter);
    }

    private void reset(Spinner spinner) {
        if (spinner.getCount() > 0) {
            spinner.setSelection(0);
        }
    }

    private int getIntSelect(Spinner spinner) {
        return (int) getSelect(spinner);
    }

    private long getSelect(Spinner spinner) {
        return SimpleSpinnerAdapter.getSelect(spinner);
    }

    private String getSelectText(Spinner spinner) {
        return SimpleSpinnerAdapter.getSelectText(spinner);
    }

    protected String text(EditText editText) {
        CharSequence charSequence = editText.getText();
        if (charSequence == null) {
            return null;
        }
        return charSequence.toString();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_search) {
            hideFavorites(true);
        } else if (v.getId() == R.id.btn_reset) {
            resetAll();
        }
    }

    public void search(String message) {
        if (TextUtils.isEmpty(message)) {
            message = "";
        }
        keyWord.setText(message);
        search();
    }

    private void search() {
        if (mICardSearcher != null) {
            CardSearchInfo searchInfo = new CardSearchInfo.Builder()
                    .keyword(text(keyWord))
                    .attribute(getIntSelect(attributeSpinner))
                    .level(getIntSelect(levelSpinner))
                    .race(getSelect(raceSpinner))
                    .atk(text(atkText))
                    .def(text(defText))
                    .pscale(getIntSelect(pScale))
                    .limitType(getIntSelect(limitSpinner))
                    .limitName(getSelectText(limitListSpinner))
                    .setcode(getSelect(setCodeSpinner))
                    .category(getSelect(categorySpinner))
                    .ot(getIntSelect(otSpinner))
                    .types(new long[]{
                            getSelect(typeSpinner),
                            getSelect(typeMonsterSpinner),
                            getSelect(typeSpellSpinner),
                            getSelect(typeTrapSpinner),
                            getSelect(typeMonsterSpinner2)
                    })
                    .linkKey(lineKey)
                    .build();
            Log.i(TAG, searchInfo.toString());
            mICardSearcher.search(searchInfo);
            lineKey = 0;
        }
    }

    private void resetAll() {
        if (mICardSearcher != null) {
            mICardSearcher.onReset();
        }
        keyWord.setText(null);
        reset(otSpinner);
        reset(limitSpinner);
//        reset(limitListSpinner);
        if (limitListSpinner.getAdapter().getCount() > 1) {
            limitListSpinner.setSelection(1);
        }
        reset(limitSpinner);
        reset(typeSpinner);
        reset(typeSpellSpinner);
        reset(typeTrapSpinner);
        reset(setCodeSpinner);
        reset(categorySpinner);
        resetMonster();
    }

    private void resetMonster() {
        reset(pScale);
        reset(typeMonsterSpinner);
        reset(typeMonsterSpinner2);
        reset(raceSpinner);
        reset(levelSpinner);
        reset(attributeSpinner);
        atkText.setText(null);
        defText.setText(null);
    }

    public interface CallBack {
        void onSearchStart();

        void onSearchResult(List<Card> Cards, boolean isHide);
    }
}
