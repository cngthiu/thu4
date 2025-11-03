// src/main/resources/static/js/adminlte.js

(function ($) {
    "use strict";

    var PushMenu = {
        options: {
            collapseScreenSize: 767,
        },
        initialize: function () {
            this.bindEvents();
            this.setupMenu();
        },
        bindEvents: function () {
            // Thêm listener cho nút toggle
            $(document).on('click', '[data-widget="pushmenu"]', function (e) {
                e.preventDefault();
                this.toggle();
            }.bind(this));

            // Tự động thu gọn khi thay đổi kích thước cửa sổ
            $(window).on('resize', function () {
                this.setupMenu();
            }.bind(this));
        },
        setupMenu: function() {
            if ($(window).width() <= this.options.collapseScreenSize) {
                this.collapse();
            } else {
                this.expand();
            }
        },
        toggle: function () {
            if ($('body').hasClass('sidebar-collapse')) {
                this.expand();
            } else {
                this.collapse();
            }
        },
        expand: function () {
            $('body').removeClass('sidebar-collapse');
        },
        collapse: function () {
            $('body').addClass('sidebar-collapse');
        }
    };

    // Khởi tạo
    $(function () {
        PushMenu.initialize();
    });

})(jQuery);