package com.paypocket.controller;

import com.paypocket.dto.TransferResult;
import com.paypocket.exception.PayPocketException;
import com.paypocket.model.Currency;
import com.paypocket.model.Transaction;
import com.paypocket.model.User;
import com.paypocket.model.Wallet;
import com.paypocket.service.UserService;
import com.paypocket.service.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;

    public WalletController(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    // ========================
    // ДАШБОРД
    // ========================

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session,
                            Model model) {
        User user = getCurrentUser(session);
        if  (user == null) {
            return "redirect:/login";
        }

        List<Wallet> wallets = walletService.getUserWallets(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("wallets", wallets);
        return "dashboard";
    }

    // ========================
    // СОЗДАНИЕ КОШЕЛЬКА
    // ========================

    @PostMapping("/wallets/create")
    public String createWallet(@RequestParam String name,
                               @RequestParam String currency,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User  user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Currency cur =  Currency.valueOf(currency.toUpperCase());
            walletService.createWallet(user.getId(), name, cur);
            redirectAttributes.addFlashAttribute("success", "Кошелек создан");
        } catch (PayPocketException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Неизвестная валюта: " + currency.toUpperCase());
        }

        return "redirect:/dashboard";
    }

    // ========================
    // ПОПОЛНЕНИЕ КОШЕЛЬКА
    // ========================

    @GetMapping("/deposit")
    public String showDepositPage(HttpSession session,
                          Model model) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        List<Wallet> wallets = walletService.getUserWallets(user.getId());
        model.addAttribute("wallets", wallets);
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam UUID walletId,
                          @RequestParam BigDecimal amount,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            walletService.deposit(walletId, amount);
            redirectAttributes.addFlashAttribute("success", "Кошелек пополнен на " + amount);
        } catch (PayPocketException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return  "redirect:/dashboard";
    }

    // ========================
    // ПЕРЕВОД
    // ========================

    @GetMapping("/transfer")
    public String showTransferPage(HttpSession session,
                                   Model model) {
        User  user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        List<Wallet> wallets = walletService.getUserWallets(user.getId());
        model.addAttribute("wallets", wallets);
        return "transfer";
    }

    @PostMapping("/transfer")
    public String transfer(@RequestParam UUID fromWalletId,
                           @RequestParam String recipientUsername,
                           @RequestParam BigDecimal amount,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            User recipient = userService.getByUsername(recipientUsername);

            Wallet senderWallet = walletService.getWallet(fromWalletId);
            Wallet receiverWallet = walletService.getUserWallets(recipient.getId()).stream()
                    .filter(w -> w.getCurrency() == senderWallet.getCurrency())
                    .findFirst()
                    .orElseThrow(() -> new PayPocketException("У получателя нет кошелька в валюте " +  senderWallet.getCurrency()));

            TransferResult result = walletService.transfer(fromWalletId, receiverWallet.getId(), amount);

            redirectAttributes.addFlashAttribute("success",
                    String.format("Переведено %s %s пользователю %s",
                            amount, senderWallet.getCurrency(), recipientUsername));
        } catch (PayPocketException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/dashboard";
    }

    // ========================
    // ИСТОРИЯ ОПЕРАЦИЙ
    // ========================

    @GetMapping("/history/{walletId}")
    public String history(@PathVariable UUID walletId,
                          @RequestParam(defaultValue = "0") int page,
                          HttpSession session,
                          Model model) {
        User user = getCurrentUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Wallet wallet = walletService.getWallet(walletId);
        Page<Transaction> transactions = walletService.getTransactionHistory(
                walletId, page, 10);

        model.addAttribute("wallet", wallet);
        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);

        return "history";
    }

    // ========================
    // Вспомогательный метод
    // ========================

    private User getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        return user;
    }

}
