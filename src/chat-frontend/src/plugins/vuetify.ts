import { createVuetify } from 'vuetify';
import '@mdi/font/css/materialdesignicons.css';
import * as directives from 'vuetify/directives';

//DragScroll
import { VueDraggableNext } from 'vue-draggable-next';

// import { BLUE_THEME} from '@/theme/LightTheme';
import { AQUA_THEME, BLUE_THEME, CYAN_THEME, GREEN_THEME, ORANGE_THEME, PURPLE_THEME } from '@/theme/LightTheme';
import {
    DARK_AQUA_THEME,
    DARK_BLUE_THEME,
    DARK_CYAN_THEME,
    DARK_GREEN_THEME,
    DARK_ORANGE_THEME,
    DARK_PURPLE_THEME
} from '@/theme/DarkTheme';

export default createVuetify({
    components: {
        draggable: VueDraggableNext
    },
    directives,

    theme: {
        defaultTheme: 'AQUA_THEME',
        themes: {
            BLUE_THEME,
            AQUA_THEME,
            PURPLE_THEME,
            GREEN_THEME,
            CYAN_THEME,
            ORANGE_THEME,
            DARK_BLUE_THEME,
            DARK_AQUA_THEME,
            DARK_ORANGE_THEME,
            DARK_PURPLE_THEME,
            DARK_GREEN_THEME,
            DARK_CYAN_THEME
        }
    },
    defaults: {
        VAutocomplete: {
            variant: 'outlined',
            density: 'compact',
            color: 'primary'
        },
        VCard: {
            rounded: 'md',
            color: 'cardBg'
        },
        VFileInput: {
            variant: 'outlined',
            density: 'compact',
            color: 'primary'
        },
        VInput: {
            variant: 'outlined',
            density: 'compact',
            color: 'primary'
        },
        VTextField: {
            variant: 'outlined',
            density: 'compact',
            color: 'primary'
        },
        VTextarea: {
            variant: 'outlined',
            density: 'compact',
            color: 'primary'
        },
        VSelect: {
            variant: 'outlined',
            density: 'comfortable',
            color: 'primary'
        },
        VListItem: {
            minHeight: '45px'
        },
        VTooltip: {
            location: 'top'
        }
    }
});
