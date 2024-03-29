package io.mindspice.toastit.shell;

import io.mindspice.mindlib.data.tuples.Pair;
import io.mindspice.toastit.entries.task.TaskEntry;
import io.mindspice.toastit.util.Util;

import java.util.Collections;
import java.util.List;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class InputPrompt<T> {
    private List<Pair<Integer, T>> allItems;
    private List<Pair<Integer, T>> filtered;

    public InputPrompt(List<T> items) {
        allItems = createdIndexed(items);
        filtered = allItems;
    }

    private List<Pair<Integer, T>> createdIndexed(List<T> items) {
        return IntStream.range(0, items.size())
                .mapToObj(i -> Pair.of(i, items.get(i))).collect(Collectors.toList());
    }

    public void resetFiltered() {
        filtered = allItems;
    }

    public List<Pair<Integer, T>> getFiltered() {
        return filtered;
    }

    public List<T> getItems() {
        return allItems.stream().map(Pair::second).toList();
    }

    public List<Pair<Integer, T>> getIndexedItems() {
        return Collections.unmodifiableList(allItems);
    }

    public void replaceItems(List<T> items) {
        allItems = createdIndexed(items);
        filtered = allItems;
    }

    public void updateAll(UnaryOperator<T> operator) {
        allItems = allItems.stream().map(i -> Pair.of(i.first(), operator.apply(i.second()))).toList();
    }

    public void addItem(T item) {
        allItems.add(Pair.of(!allItems.isEmpty() ? allItems.getLast().first() + 1 : 0, item));
    }

    public void removeItems(Predicate<Pair<Integer, T>> predicate) {
        allItems.removeIf(predicate);
        filtered = allItems;
    }

    public Prompt create() {
        return new Prompt();
    }

    public class Prompt {
        private BooleanSupplier inputLenPred = null;
        private BooleanSupplier indexPred = null;
        private BooleanSupplier confirmPred = null;
        private Consumer<T> itemConsumer = null;
        private UnaryOperator<T> itemUpdate;
        private Consumer<List<Pair<Integer, T>>> listAction;
        private Predicate<T> filter;
        private Function<List<T>, T> itemSelector;
        boolean listRemove;
        Runnable waitPrompt;
        private T item;
        private int index = -1;

        public Prompt validateAndGetIndex(String input) {
            indexPred = () -> {
                if (Util.isInt(input)) {
                    int index = Integer.parseInt(input);
                    if (index >= 0 && index < allItems.size()) {
                        this.item = allItems.get(index).second();
                        this.index = index;
                        return true;
                    }
                }
                return false;
            };
            return this;
        }

        public Prompt validateInputLength(String[] input, int i) {
            inputLenPred = () -> input.length >= i;
            return this;
        }

        public Prompt confirm(Function<String, Boolean> confirmFunc, Function<T, String> textFunc) {
            this.confirmPred = () -> confirmFunc.apply(textFunc.apply(item));
            return this;
        }

        public Prompt itemConsumer(Consumer<T> consumer) {
            this.itemConsumer = consumer;
            return this;
        }

        public Prompt itemUpdate(UnaryOperator<T> selfAction) {
            this.itemUpdate = selfAction;
            return this;
        }

        public Prompt forceSelect(T forceItem) {
            Pair<Integer, T> selection = allItems.stream().filter(i -> i.second().equals(forceItem)).findFirst().orElse(null);
            for (int i = 0; i < allItems.size(); ++i) {
                if (allItems.get(i).second().equals(forceItem)) {
                    item = allItems.get(i).second();
                    index = i;
                }
            }
            return this;
        }

        public Prompt filter(Predicate<T> predicate) {
            filter = predicate;
            return this;
        }

        public int selectedIndex() {
            return index;
        }

        public Prompt listAction(Consumer<List<Pair<Integer, T>>> consumer) {
            this.listAction = consumer;
            return this;
        }

        public Prompt listRemove() {
            listRemove = true;
            return this;
        }

        public Prompt waitPrompt(Runnable waitPrompt) {
            this.waitPrompt = waitPrompt;
            return this;
        }

        public T getItem() {
            return item;
        }

        public String display(Function<T, String> printFunc) {
            if (inputLenPred != null && !inputLenPred.getAsBoolean()) {
                return "Invalid Input";
            }

            if (indexPred != null && !indexPred.getAsBoolean()) {
                return "Invalid Index";
            }

            if (itemSelector != null) {
                item = itemSelector.apply(getItems());
                if (item == null) {
                    return "No Item Found";
                }
            }

            if (confirmPred != null && !confirmPred.getAsBoolean()) {
                return "";
            }

            if (filter != null) {
                filtered = allItems.stream()
                        .filter(t -> filter.test(t.second()))
                        .toList();
            }

            if (itemUpdate != null) {
                if (item == null || index == -1) {
                    return "No Item Selected";
                }
                item = itemUpdate.apply(item);
                allItems.set(index, Pair.of(index, item));

            }

            if (itemConsumer != null) {
                if (item == null) {
                    return "No Item Selected";
                }
                itemConsumer.accept(item);
            }

            if (listAction != null) {
                listAction.accept(allItems);
            }

            if (listRemove) {
                allItems.remove(index);
            }

            if (waitPrompt != null) {
                waitPrompt.run();
            }
            return printFunc.apply(item);

        }

    }
}