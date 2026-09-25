package com.calctastic.sample.hypercal.display.model;

import com.calctastic.sample.hypercal.display.renderer.RenderContext;
import com.calctastic.sample.hypercal.display.renderer.TokenRendererRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * MathFormulaDocument:
 * Representasi dokumen formula dan manipulasi kursor/token (setara ZB.java, C0351xb.java, dan C0091Sd.java di HiPER).
 * Bertanggung jawab penuh terhadap:
 * 1. Penyimpanan daftar pohon token (AST).
 * 2. Posisi kursor utama dan sub-kursor container (Pecahan, Akar, Pangkat).
 * 3. Operasi pengetikan angka, operator, pecahan, akar, pangkat, dan penghapusan token.
 * 4. Navigasi kursor kiri/kanan dan tap selection.
 */
public class MathFormulaDocument {

    public interface OnDocumentChangeListener {
        void onDocumentChanged();
    }

    private final List<MathToken> tokens = new ArrayList<>();
    private int cursorIndex = 0;

    // Container focus (Fraction, Sqrt, Power) - Sesuai C0157eA.java (node pointer)
    private int containerFocusIndex = -1;
    private MathToken containerFocusToken = null;
    private boolean containerInSecondary = false; // true untuk penyebut (fraction) atau eksponen (power)
    private int containerSubCursor = 0;

    private OnDocumentChangeListener changeListener;

    public void setOnDocumentChangeListener(OnDocumentChangeListener listener) {
        this.changeListener = listener;
    }

    private void notifyChange() {
        if (changeListener != null) {
            changeListener.onDocumentChanged();
        }
    }

    public List<MathToken> getTokens() {
        return tokens;
    }

    public int getCursorIndex() {
        return cursorIndex;
    }

    public int getContainerFocusIndex() {
        return containerFocusIndex;
    }

    public MathToken getContainerFocusToken() {
        return containerFocusToken;
    }

    public boolean isContainerInSecondary() {
        return containerInSecondary;
    }

    public int getContainerSubCursor() {
        return containerSubCursor;
    }

    // Kompatibilitas dengan fraction getters
    public int getFractionFocusIndex() {
        if (containerFocusToken != null) {
            return containerFocusToken.type == MathToken.Type.FRACTION ? containerFocusIndex : -1;
        }
        if (containerFocusIndex >= 0 && containerFocusIndex < tokens.size()) {
            if (tokens.get(containerFocusIndex).type == MathToken.Type.FRACTION) {
                return containerFocusIndex;
            }
        }
        return -1;
    }

    public boolean isFractionInDenominator() {
        return containerInSecondary;
    }

    public int getFractionSubCursor() {
        return containerSubCursor;
    }

    private List<MathToken> getActiveTargetList() {
        MathToken container = containerFocusToken;
        if (container == null && containerFocusIndex >= 0 && containerFocusIndex < tokens.size()) {
            container = tokens.get(containerFocusIndex);
        }
        if (container != null) {
            if (container.type == MathToken.Type.FRACTION || container.type == MathToken.Type.POWER) {
                return containerInSecondary ? container.secondaryChildren : container.children;
            } else if (container.type == MathToken.Type.SQRT) {
                return container.children;
            }
        }
        return null;
    }

    public void appendText(String text) {
        if (text == null || text.isEmpty()) return;

        List<MathToken> target = getActiveTargetList();
        if (target != null) {
            if (containerSubCursor > 0 && containerSubCursor <= target.size()) {
                MathToken prev = target.get(containerSubCursor - 1);
                if (prev.type == MathToken.Type.NUMBER) {
                    prev.text = prev.text + text;
                } else {
                    target.add(containerSubCursor, MathToken.number(text));
                    containerSubCursor++;
                }
            } else if (containerSubCursor == 0 && !target.isEmpty()) {
                MathToken first = target.get(0);
                if (first.type == MathToken.Type.NUMBER) {
                    first.text = text + first.text;
                } else {
                    target.add(0, MathToken.number(text));
                }
                containerSubCursor++;
            } else {
                target.add(containerSubCursor, MathToken.number(text));
                containerSubCursor++;
            }
            notifyChange();
            return;
        }

        if (cursorIndex > 0 && cursorIndex <= tokens.size()) {
            MathToken prev = tokens.get(cursorIndex - 1);
            if (prev.type == MathToken.Type.NUMBER) {
                prev.text = prev.text + text;
                notifyChange();
                return;
            }
        } else if (cursorIndex == 0 && !tokens.isEmpty()) {
            MathToken first = tokens.get(0);
            if (first.type == MathToken.Type.NUMBER) {
                first.text = text + first.text;
                cursorIndex++;
                notifyChange();
                return;
            }
        }

        tokens.add(cursorIndex, MathToken.number(text));
        cursorIndex++;
        notifyChange();
    }

    public void appendOperator(String op) {
        if (op == null || op.isEmpty()) return;

        List<MathToken> target = getActiveTargetList();
        if (target != null) {
            target.add(containerSubCursor, MathToken.operator(op));
            containerSubCursor++;
            notifyChange();
            return;
        }

        tokens.add(cursorIndex, MathToken.operator(op));
        cursorIndex++;
        notifyChange();
    }

    public void appendToken(MathToken token) {
        if (token == null) return;
        List<MathToken> target = getActiveTargetList();
        if (target != null) {
            target.add(containerSubCursor, token);
            containerSubCursor++;
            notifyChange();
            return;
        }
        tokens.add(cursorIndex, token);
        cursorIndex++;
        notifyChange();
    }

    public void appendFraction() {
        List<MathToken> target = getActiveTargetList();
        if (target != null) {
            MathToken prev = null;
            if (containerSubCursor > 0 && containerSubCursor <= target.size()) {
                prev = target.remove(containerSubCursor - 1);
                containerSubCursor--;
            }
            List<MathToken> numList = new ArrayList<>();
            if (prev != null) numList.add(prev);
            MathToken fracToken = MathToken.fraction(numList, new ArrayList<>());
            target.add(containerSubCursor, fracToken);
            containerFocusToken = fracToken;
            containerInSecondary = (prev != null);
            containerSubCursor = 0;
            notifyChange();
            return;
        }

        MathToken prev = null;
        if (cursorIndex > 0 && cursorIndex <= tokens.size()) {
            prev = tokens.remove(cursorIndex - 1);
            cursorIndex--;
        }

        List<MathToken> numList = new ArrayList<>();
        if (prev != null) {
            numList.add(prev);
        }

        MathToken fracToken = MathToken.fraction(numList, new ArrayList<>());
        tokens.add(cursorIndex, fracToken);

        containerFocusIndex = cursorIndex;
        containerFocusToken = fracToken;
        if (prev != null) {
            containerInSecondary = true; // Langsung ke penyebut jika pembilang terisi angka sebelumnya
            containerSubCursor = 0;
        } else {
            containerInSecondary = false; // Ke kotak pembilang atas jika pecahan baru
            containerSubCursor = 0;
        }

        notifyChange();
    }

    public void appendReciprocal() {
        List<MathToken> target = getActiveTargetList();
        if (target != null) {
            MathToken prev = null;
            if (containerSubCursor > 0 && containerSubCursor <= target.size()) {
                prev = target.remove(containerSubCursor - 1);
                containerSubCursor--;
            }
            List<MathToken> denList = new ArrayList<>();
            if (prev != null) denList.add(prev);
            List<MathToken> numList = new ArrayList<>();
            numList.add(MathToken.number("1"));
            MathToken fracToken = MathToken.fraction(numList, denList);
            target.add(containerSubCursor, fracToken);
            containerFocusToken = fracToken;
            containerInSecondary = true;
            containerSubCursor = denList.size();
            notifyChange();
            return;
        }

        MathToken prev = null;
        if (cursorIndex > 0 && cursorIndex <= tokens.size()) {
            prev = tokens.remove(cursorIndex - 1);
            cursorIndex--;
        }

        List<MathToken> denList = new ArrayList<>();
        if (prev != null) {
            denList.add(prev);
        }

        // Pembilang selalu 1
        List<MathToken> numList = new ArrayList<>();
        numList.add(MathToken.number("1"));

        MathToken fracToken = MathToken.fraction(numList, denList);
        tokens.add(cursorIndex, fracToken);

        containerFocusIndex = cursorIndex;
        containerFocusToken = fracToken;
        containerInSecondary = true; // Kursor berada di penyebut (bawah) untuk mengisi nilai x
        containerSubCursor = denList.size();

        notifyChange();
    }

    public void appendSqrt() {
        List<MathToken> target = getActiveTargetList();
        if (target != null) {
            MathToken sqrtToken = MathToken.sqrt(new ArrayList<>());
            target.add(containerSubCursor, sqrtToken);
            containerFocusToken = sqrtToken;
            containerInSecondary = false;
            containerSubCursor = 0;
            notifyChange();
            return;
        }

        MathToken sqrtToken = MathToken.sqrt(new ArrayList<>());
        tokens.add(cursorIndex, sqrtToken);
        containerFocusIndex = cursorIndex;
        containerFocusToken = sqrtToken;
        containerInSecondary = false;
        containerSubCursor = 0;
        notifyChange();
    }

    public void appendPower() {
        List<MathToken> target = getActiveTargetList();
        if (target != null) {
            MathToken prev = null;
            if (containerSubCursor > 0 && containerSubCursor <= target.size()) {
                prev = target.remove(containerSubCursor - 1);
                containerSubCursor--;
            }
            List<MathToken> baseList = new ArrayList<>();
            if (prev != null) {
                if (prev.type == MathToken.Type.PAREN_GROUP) {
                    baseList.add(prev);
                } else {
                    baseList.add(MathToken.paren(List.of(prev)));
                }
            }
            MathToken powerToken = MathToken.power(baseList, new ArrayList<>());
            target.add(containerSubCursor, powerToken);
            containerFocusToken = powerToken;
            containerInSecondary = true;
            containerSubCursor = 0;
            notifyChange();
            return;
        }

        MathToken prev = null;
        if (cursorIndex > 0 && cursorIndex <= tokens.size()) {
            prev = tokens.remove(cursorIndex - 1);
            cursorIndex--;
        }

        List<MathToken> baseList = new ArrayList<>();
        if (prev != null) {
            // HiPER C0349xH / ZB logic: bungkus basis ke dalam tanda kurung ( ... )
            if (prev.type == MathToken.Type.PAREN_GROUP) {
                baseList.add(prev);
            } else {
                baseList.add(MathToken.paren(List.of(prev)));
            }
        }

        MathToken powerToken = MathToken.power(baseList, new ArrayList<>());
        tokens.add(cursorIndex, powerToken);
        containerFocusIndex = cursorIndex;
        containerFocusToken = powerToken;
        containerInSecondary = true; // Fokus langsung ke kotak eksponen atas
        containerSubCursor = 0;
        notifyChange();
    }

    public void appendSquare() {
        List<MathToken> target = getActiveTargetList();
        if (target != null) {
            MathToken prev = null;
            if (containerSubCursor > 0 && containerSubCursor <= target.size()) {
                prev = target.remove(containerSubCursor - 1);
                containerSubCursor--;
            }
            List<MathToken> baseList = new ArrayList<>();
            if (prev != null) {
                if (prev.type == MathToken.Type.PAREN_GROUP) {
                    baseList.add(prev);
                } else {
                    baseList.add(MathToken.paren(List.of(prev)));
                }
            }
            MathToken powerToken = MathToken.power(baseList, List.of(MathToken.number("2")));
            target.add(containerSubCursor, powerToken);
            containerSubCursor++;
            notifyChange();
            return;
        }

        MathToken prev = null;
        if (cursorIndex > 0 && cursorIndex <= tokens.size()) {
            prev = tokens.remove(cursorIndex - 1);
            cursorIndex--;
        }

        List<MathToken> baseList = new ArrayList<>();
        if (prev != null) {
            // HiPER C0349xH / ZB logic: bungkus basis ke dalam tanda kurung ( ... )
            if (prev.type == MathToken.Type.PAREN_GROUP) {
                baseList.add(prev);
            } else {
                baseList.add(MathToken.paren(List.of(prev)));
            }
        }

        // Eksponen bernilai 2
        MathToken powerToken = MathToken.power(baseList, List.of(MathToken.number("2")));
        tokens.add(cursorIndex, powerToken);
        cursorIndex++;
        containerFocusIndex = -1;
        containerFocusToken = null;
        containerInSecondary = false;
        containerSubCursor = 0;
        notifyChange();
    }

    public void deleteBackward() {
        if (containerFocusIndex >= 0 && containerFocusIndex < tokens.size()) {
            MathToken container = tokens.get(containerFocusIndex);
            List<MathToken> target = getActiveTargetList();

            if (target != null && containerSubCursor > 0 && containerSubCursor <= target.size()) {
                MathToken t = target.get(containerSubCursor - 1);
                if (t.type == MathToken.Type.NUMBER && t.text.length() > 1) {
                    t.text = t.text.substring(0, t.text.length() - 1);
                } else {
                    target.remove(containerSubCursor - 1);
                    containerSubCursor--;
                }
            } else if (container.type == MathToken.Type.FRACTION && containerInSecondary) {
                containerInSecondary = false;
                containerSubCursor = container.children.size();
            } else {
                tokens.remove(containerFocusIndex);
                cursorIndex = containerFocusIndex;
                containerFocusIndex = -1;
                containerFocusToken = null;
            }
            notifyChange();
            return;
        }

        if (cursorIndex > 0 && cursorIndex <= tokens.size()) {
            MathToken token = tokens.get(cursorIndex - 1);
            if (token.type == MathToken.Type.NUMBER && token.text.length() > 1) {
                token.text = token.text.substring(0, token.text.length() - 1);
            } else {
                tokens.remove(cursorIndex - 1);
                cursorIndex--;
            }
            notifyChange();
        }
    }

    public void clearAll() {
        tokens.clear();
        cursorIndex = 0;
        containerFocusIndex = -1;
        containerFocusToken = null;
        containerInSecondary = false;
        containerSubCursor = 0;
        notifyChange();
    }

    public void moveCursorLeft() {
        if (containerFocusIndex >= 0 && containerFocusIndex < tokens.size()) {
            MathToken container = containerFocusToken != null ? containerFocusToken : tokens.get(containerFocusIndex);
            if (containerSubCursor > 0) {
                containerSubCursor--;
            } else if (container.type == MathToken.Type.FRACTION && containerInSecondary) {
                containerInSecondary = false;
                containerSubCursor = container.children.size();
            } else {
                cursorIndex = containerFocusIndex;
                containerFocusIndex = -1;
                containerFocusToken = null;
            }
            notifyChange();
            return;
        }

        if (cursorIndex > 0) {
            cursorIndex--;
            MathToken prev = tokens.get(cursorIndex);
            if (prev.type == MathToken.Type.FRACTION) {
                containerFocusIndex = cursorIndex;
                containerFocusToken = prev;
                containerInSecondary = true;
                containerSubCursor = prev.secondaryChildren.size();
            } else if (prev.type == MathToken.Type.SQRT) {
                containerFocusIndex = cursorIndex;
                containerFocusToken = prev;
                containerInSecondary = false;
                containerSubCursor = prev.children.size();
            } else if (prev.type == MathToken.Type.POWER) {
                containerFocusIndex = cursorIndex;
                containerFocusToken = prev;
                containerInSecondary = true;
                containerSubCursor = prev.secondaryChildren.size();
            }
            notifyChange();
        }
    }

    public void moveCursorRight() {
        if (containerFocusIndex >= 0 && containerFocusIndex < tokens.size()) {
            MathToken container = containerFocusToken != null ? containerFocusToken : tokens.get(containerFocusIndex);
            List<MathToken> target = getActiveTargetList();
            if (target != null && containerSubCursor < target.size()) {
                containerSubCursor++;
            } else if (container.type == MathToken.Type.FRACTION && !containerInSecondary) {
                containerInSecondary = true;
                containerSubCursor = 0;
            } else {
                cursorIndex = containerFocusIndex + 1;
                containerFocusIndex = -1;
                containerFocusToken = null;
            }
            notifyChange();
            return;
        }

        if (cursorIndex < tokens.size()) {
            MathToken next = tokens.get(cursorIndex);
            if (next.type == MathToken.Type.FRACTION) {
                containerFocusIndex = cursorIndex;
                containerFocusToken = next;
                containerInSecondary = false;
                containerSubCursor = 0;
            } else if (next.type == MathToken.Type.SQRT) {
                containerFocusIndex = cursorIndex;
                containerFocusToken = next;
                containerInSecondary = false;
                containerSubCursor = 0;
            } else if (next.type == MathToken.Type.POWER) {
                containerFocusIndex = cursorIndex;
                containerFocusToken = next;
                containerInSecondary = true;
                containerSubCursor = 0;
            } else {
                cursorIndex++;
            }
            notifyChange();
        }
    }

    public void handleTap(float touchX, float touchY, float startX, TokenRendererRegistry registry,
                          float baseTextSize, RenderContext ctx, List<RenderContext.FractionHitBox> hitBoxes,
                          List<RenderContext.ContainerHitBox> containerHitBoxes) {
        // Cek klik pada wadah pecahan (pembilang / penyebut)
        if (hitBoxes != null) {
            for (RenderContext.FractionHitBox box : hitBoxes) {
                MathToken frac = box.targetToken;
                if (frac == null && box.tokenIndex >= 0 && box.tokenIndex < tokens.size()) {
                    frac = tokens.get(box.tokenIndex);
                }
                if (frac == null) continue;

                if (box.numBox.contains(touchX, touchY)) {
                    containerFocusIndex = box.tokenIndex;
                    containerFocusToken = frac;
                    containerInSecondary = false;
                    if (frac.children.isEmpty()) {
                        containerSubCursor = 0;
                    } else {
                        // HiPER Qg.java: Hitung apakah tap di sebelah kiri atau kanan bilangan
                        float midX = box.numStartX + (box.numWidth / 2f);
                        containerSubCursor = (touchX < midX) ? 0 : frac.children.size();
                    }
                    notifyChange();
                    return;
                } else if (box.denBox.contains(touchX, touchY)) {
                    containerFocusIndex = box.tokenIndex;
                    containerFocusToken = frac;
                    containerInSecondary = true;
                    if (frac.secondaryChildren.isEmpty()) {
                        containerSubCursor = 0;
                    } else {
                        // HiPER Qg.java: Hitung apakah tap di sebelah kiri atau kanan bilangan
                        float midX = box.denStartX + (box.denWidth / 2f);
                        containerSubCursor = (touchX < midX) ? 0 : frac.secondaryChildren.size();
                    }
                    notifyChange();
                    return;
                }
            }
        }

        // Cek klik pada wadah umum (akar, eksponen pangkat)
        if (containerHitBoxes != null) {
            for (RenderContext.ContainerHitBox box : containerHitBoxes) {
                if (box.bounds.contains(touchX, touchY)) {
                    MathToken token = box.targetToken;
                    if (token == null && box.tokenIndex >= 0 && box.tokenIndex < tokens.size()) {
                        token = tokens.get(box.tokenIndex);
                    }
                    if (token == null) continue;

                    containerFocusIndex = box.tokenIndex;
                    containerFocusToken = token;
                    containerInSecondary = box.isSecondary;
                    List<MathToken> target = box.isSecondary ? token.secondaryChildren : token.children;
                    if (target.isEmpty()) {
                        containerSubCursor = 0;
                    } else {
                        // Perilaku HiPER: jika tap di kiri dari titik tengah bilangan, taruh di awal (0), jika kanan di akhir (target.size())
                        float midX = box.contentStartX + (box.contentWidth / 2f);
                        containerSubCursor = (touchX < midX) ? 0 : target.size();
                    }
                    notifyChange();
                    return;
                }
            }
        }

        // Klik di ekspresi utama (luar wadah)
        containerFocusIndex = -1;
        containerFocusToken = null;
        int nearestIndex = tokens.size();
        float minDiff = Float.MAX_VALUE;

        float curX = startX;
        for (int i = 0; i < tokens.size(); i++) {
            float tokenW = registry.measureWidth(tokens.get(i), baseTextSize, ctx);
            float midX = curX + (tokenW / 2f);
            if (touchX < midX) {
                nearestIndex = i;
                break;
            }
            curX += tokenW;
            nearestIndex = i + 1;
        }

        cursorIndex = nearestIndex;
        notifyChange();
    }
}
