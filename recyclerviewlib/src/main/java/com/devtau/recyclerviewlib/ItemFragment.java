package com.devtau.recyclerviewlib;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.devtau.recyclerviewlib.util.Constants;
import com.devtau.recyclerviewlib.util.Logger;
import java.util.ArrayList;
import java.util.List;
/**
 * Фрагмент обобщающий сортировку, вставку новой записи с отображением самого списка
 */
public class ItemFragment<T extends Parcelable> extends Fragment implements
        SortAndAddFragment.OnSortAndAddFragmentListener,
        RVFragment.OnRVFragmentListener<T> {
    public static final String ARG_ITEMS_LIST = "itemsList";
    public static final String ARG_COLUMN_COUNT = "columnCount";
    public static final String ARG_LIST_ITEM_LAYOUT_ID = "listItemLayoutId";
    public static final String ARG_SORT_BY = "sortBy";
    private OnItemFragmentListener listener;
    private SortBy sortBy = Constants.DEFAULT_SORT_BY;

    private SortAndAddFragment sortAndAddFragment;
    private RVFragment rvFragment;

    //Обязательный пустой конструктор
    public ItemFragment() { }
    //мы не можем использовать статический метод newInstance() для создания фрагмента с дженериками

    @Override
    public void onAttach(Context context) {
        //если фрагмент является вложенным, context - это активность, держащая фрагмент-родитель, а не сам родитель
        Logger.d("ItemFragment.onAttach()");
        super.onAttach(context);
        try {
            //проверим, реализован ли нужный интерфейс родительским фрагментом или активностью
            listener = (OnItemFragmentListener) getParentFragment();
            if (listener == null) {
                listener = (OnItemFragmentListener) context;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnItemFragmentListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.d("ItemFragment.onCreateView()");
        View view = inflater.inflate(R.layout.fragment_rv_helper, container, false);

        //рекомендовано читать аргументы из бандла не здесь, а в onCreate, но тогда нам нужно создавать
        //все переменные на уровне фрагмента, а не локальные для метода, которые мы сразу отдаем в адаптер
        ArrayList<T> itemsList = new ArrayList<>();
        int columnCount = Constants.DEFAULT_COLUMN_COUNT;
        int listItemLayoutId = Constants.DEFAULT_LIST_ITEM_LAYOUT;
        SortBy sortBy = Constants.DEFAULT_SORT_BY;

        if (getArguments() != null) {
            itemsList = getArguments().getParcelableArrayList(ARG_ITEMS_LIST);
            columnCount = getArguments().getInt(ARG_COLUMN_COUNT);
            listItemLayoutId = getArguments().getInt(ARG_LIST_ITEM_LAYOUT_ID);
            sortBy = (SortBy) getArguments().getSerializable(ARG_SORT_BY);
        }

        sortAndAddFragment = createSortAndAddFragment(sortBy);
        rvFragment = createRVFragment(itemsList, columnCount, listItemLayoutId, sortBy);
        addFragmentToLayout(R.id.sort_and_add_placeholder, sortAndAddFragment);
        addFragmentToLayout(R.id.recycler_view_placeholder, rvFragment);

        return view;
    }

    public SortAndAddFragment createSortAndAddFragment(SortBy sortBy) {
        SortAndAddFragment fragment = new SortAndAddFragment();
        Bundle args = new Bundle();
        args.putSerializable(ItemFragment.ARG_SORT_BY, sortBy);
        fragment.setArguments(args);
        return fragment;
    }

    public RVFragment createRVFragment(ArrayList<T> itemsList, int columnCount,
                                         int listItemLayoutId, SortBy sortBy) {
        RVFragment fragment = new RVFragment();
        Bundle args = new Bundle();

        args.putParcelableArrayList(ItemFragment.ARG_ITEMS_LIST, itemsList);
        args.putInt(ItemFragment.ARG_COLUMN_COUNT, columnCount);
        args.putInt(ItemFragment.ARG_LIST_ITEM_LAYOUT_ID, listItemLayoutId);
        args.putSerializable(ItemFragment.ARG_SORT_BY, sortBy);

        fragment.setArguments(args);
        return fragment;
    }

    private void addFragmentToLayout(int placeholderID, Fragment fragment){
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(placeholderID, fragment);
        ft.commit();
    }


    //метод необходим для savedInstanceState
    public SortBy getSortByState() {
        return sortBy;
    }


    //метод публичный, т.к. при работе с бд _id хранимого объекта создается только после
    //вставки записи в бд, а к ней у списка доступа нет
    public void addItemToList(T item) {
        rvFragment.addItemToList(item, sortBy);
    }


    //методы, вызываемые, если команда на сортировку/удаление/переназначечение поступает извне списка
    //обычно такие команды генерируются внутри
    public void sortFromOutside(SortBy sortBy) {
        this.sortBy = sortBy;
        rvFragment.sort(sortBy);
    }

    public void removeItemFromList(T item) {
        rvFragment.removeItemFromList(item);
    }

    public void setList(ArrayList<T> itemsList){
        rvFragment.setList(itemsList);
    }


    @Override
    public void onDetach() {
        Logger.d("ItemFragment.onDetach()");
        super.onDetach();
        listener = null;
    }


    //аналог метода sortFromOutside(), но используется для взаимодействия между компонентами внутри ItemFragment
    @Override
    public void onSpinnerItemSelected(SortBy selectedSortBy) {
        sortBy = selectedSortBy;
        rvFragment.sort(selectedSortBy);
    }

    @Override
    public void onAddNewItemDialogResult(List<String> newItemParams) {
        listener.onAddNewItemDialogResult(newItemParams);
    }

    //этот фрагмент содержит вложенный фрагмент списка
    //можно было бы настроить прямую реализацию интерфейса взаимодействия с его дочерним списком активностью
    //однако ItemFragment может быть частью другого фрагмента, ссылку на который мы не можем получить
    @Override
    public void onListItemClick(T item, int clickedActionId) {
        if(clickedActionId == 1) {
            //обратите внимание, что удаление из списка обрабатывается внутри списка
            //клиенту нужно только передать инструкцию в бд
            rvFragment.removeItemFromList(item);
        }
        listener.onListItemClick(item, clickedActionId);
    }

    public interface OnItemFragmentListener<T extends Parcelable> {
        void onListItemClick(T item, int clickedActionId);
        void onAddNewItemDialogResult(List<String> newItemParams);
    }
}