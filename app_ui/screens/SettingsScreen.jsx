import React, { useMemo, useState } from 'react';
import { ScrollView, StyleSheet, View, Pressable } from 'react-native';
import { Appbar, Card, Divider, List, SegmentedButtons, Switch, Text, IconButton } from 'react-native-paper';
import ReactNativeHapticFeedback from 'react-native-haptic-feedback';
import SwipeableSlider from '../components/SwipeableSlider.jsx';
import { useTheme } from '../theme.jsx';
import MaterialCommunityIcons from 'react-native-vector-icons/MaterialCommunityIcons';

const hapticOptions = {
  enableVibrateFallback: true,
  ignoreAndroidSystemSettings: false,
};

function SectionCard({ title, description, icon, children }) {
  const { colors } = useTheme();
  return (
    <Card mode="contained" style={[styles.card, { backgroundColor: colors.surfaceContainer }]}>
      <View style={styles.cardHeader}>
        <View style={styles.cardHeaderLeft}>
          {icon && (
            <MaterialCommunityIcons 
              name={icon} 
              size={24} 
              color={colors.primary} 
              style={styles.cardIcon}
            />
          )}
          <View>
            <Text variant="titleMedium" style={[styles.cardTitle, { color: colors.onSurface }]}>
              {title}
            </Text>
            {description && (
              <Text variant="bodySmall" style={[styles.cardDescription, { color: colors.onSurfaceVariant }]}>
                {description}
              </Text>
            )}
          </View>
        </View>
      </View>
      <Card.Content style={styles.cardContent}>{children}</Card.Content>
    </Card>
  );
}

function SettingsScreen() {
  const { colors } = useTheme();

  const [frameLimiter, setFrameLimiter] = useState(true);
  const [fastBoot, setFastBoot] = useState(false);
  const [fpsLimit, setFpsLimit] = useState(60);
  const [brightness, setBrightness] = useState(100);
  const [aspect, setAspect] = useState('16:9');

  const [renderer, setRenderer] = useState('Vulkan');
  const [upscale, setUpscale] = useState(1);
  const [fxaa, setFxaa] = useState(false);
  const [casSharpness, setCasSharpness] = useState(50);
  const [vsync, setVsync] = useState(false);
  const [hwMipmap, setHwMipmap] = useState(false);

  const [vibration, setVibration] = useState(true);
  const [cpuCore, setCpuCore] = useState('Dynarec');

  const handleSwitchChange = (setter, value) => {
    ReactNativeHapticFeedback.trigger('impactMedium', hapticOptions);
    setter(value);
  };

  const handleSliderComplete = () => {
    ReactNativeHapticFeedback.trigger('impactLight', hapticOptions);
  };

  const surfaceStyle = useMemo(
    () => ({
      backgroundColor: colors.surfaceContainer,
      borderColor: colors.outline,
    }),
    [colors]
  );

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Appbar.Header mode="center-aligned" elevated style={{ backgroundColor: colors.surface }}>
        <Appbar.BackAction 
          onPress={() => {}} 
          color={colors.onSurface}
        />
        <Appbar.Content title="PCSX2OID Settings" titleStyle={{ fontWeight: '700' }} />
        <Appbar.Action icon="cog" onPress={() => {}} color={colors.onSurface} />
      </Appbar.Header>

      <ScrollView contentContainerStyle={styles.content}>
        <SectionCard title="General" description="Boot and overlay" icon="cog-outline">
          <List.Item
            title="Frame limiter"
            description="Keep gameplay smooth"
            left={props => <List.Icon {...props} icon="speedometer" color={colors.primary} />}
            right={() => (
              <Switch
                value={frameLimiter}
                onValueChange={(value) => handleSwitchChange(setFrameLimiter, value)}
                color={colors.primary}
              />
            )}
            style={styles.listItem}
          />
          <Divider />
          <List.Item
            title="Fast boot"
            description="Skip BIOS logo"
            left={props => <List.Icon {...props} icon="fast-forward" color={colors.primary} />}
            right={() => (
              <Switch
                value={fastBoot}
                onValueChange={(value) => handleSwitchChange(setFastBoot, value)}
                color={colors.primary}
              />
            )}
            style={styles.listItem}
          />
          <Divider />
          <View style={styles.sliderBlock}>
            <View style={styles.sliderHeader}>
              <MaterialCommunityIcons name="gauge" size={20} color={colors.primary} />
              <Text variant="titleSmall" style={{ color: colors.onSurface, marginLeft: 8 }}>
                FPS limit
              </Text>
            </View>
            <SwipeableSlider
              value={fpsLimit}
              minimumValue={30}
              maximumValue={120}
              step={5}
              onValueChange={setFpsLimit}
              onSlidingComplete={handleSliderComplete}
              colors={colors}
            />
            <Text variant="bodySmall" style={[styles.sliderValue, { color: colors.primary }]}>
              {fpsLimit} fps
            </Text>
          </View>
          <Divider />
          <View style={styles.segmentRow}>
            <View style={styles.sliderHeader}>
              <MaterialCommunityIcons name="aspect-ratio" size={20} color={colors.primary} />
              <Text variant="titleSmall" style={{ color: colors.onSurface, marginLeft: 8 }}>
                Aspect ratio
              </Text>
            </View>
            <SegmentedButtons
              value={aspect}
              onValueChange={setAspect}
              buttons={[
                { value: '16:9', label: '16:9' },
                { value: '4:3', label: '4:3' },
                { value: 'stretch', label: 'Stretch' },
              ]}
              style={styles.segmented}
            />
          </View>
          <Divider />
          <View style={styles.sliderBlock}>
            <View style={styles.sliderHeader}>
              <MaterialCommunityIcons name="brightness-6" size={20} color={colors.primary} />
              <Text variant="titleSmall" style={{ color: colors.onSurface, marginLeft: 8 }}>
                Overlay brightness
              </Text>
            </View>
            <SwipeableSlider
              value={brightness}
              minimumValue={25}
              maximumValue={125}
              step={5}
              onValueChange={setBrightness}
              onSlidingComplete={handleSliderComplete}
              colors={colors}
            />
            <Text variant="bodySmall" style={[styles.sliderValue, { color: colors.primary }]}>
              {brightness}%
            </Text>
          </View>
        </SectionCard>

        <SectionCard title="Graphics" description="Rendering defaults" icon="video-outline">
          <View style={styles.segmentRow}>
            <View style={styles.sliderHeader}>
              <MaterialCommunityIcons name="gpu" size={20} color={colors.primary} />
              <Text variant="titleSmall" style={{ color: colors.onSurface, marginLeft: 8 }}>
                Renderer
              </Text>
            </View>
            <SegmentedButtons
              value={renderer}
              onValueChange={setRenderer}
              buttons={[
                { value: 'Vulkan', label: 'Vulkan' },
                { value: 'OpenGL', label: 'OpenGL' },
                { value: 'Software', label: 'Software' },
              ]}
              style={styles.segmented}
            />
          </View>
          <Divider />
          <View style={styles.sliderBlock}>
            <View style={styles.sliderHeader}>
              <MaterialCommunityIcons name="magnify-plus-outline" size={20} color={colors.primary} />
              <Text variant="titleSmall" style={{ color: colors.onSurface, marginLeft: 8 }}>
                Upscaling
              </Text>
            </View>
            <SwipeableSlider
              value={upscale}
              minimumValue={1}
              maximumValue={6}
              step={1}
              onValueChange={setUpscale}
              onSlidingComplete={handleSliderComplete}
              colors={colors}
            />
            <Text variant="bodySmall" style={[styles.sliderValue, { color: colors.primary }]}>
              {upscale}x internal resolution
            </Text>
          </View>
          <Divider />
          <List.Item
            title="FXAA"
            description="Smooth jagged edges"
            left={props => <List.Icon {...props} icon="blur" color={colors.primary} />}
            right={() => (
              <Switch
                value={fxaa}
                onValueChange={(value) => handleSwitchChange(setFxaa, value)}
                color={colors.primary}
              />
            )}
            style={styles.listItem}
          />
          <Divider />
          <View style={styles.sliderBlock}>
            <View style={styles.sliderHeader}>
              <MaterialCommunityIcons name="image-filter-hdr" size={20} color={colors.primary} />
              <Text variant="titleSmall" style={{ color: colors.onSurface, marginLeft: 8 }}>
                CAS sharpening
              </Text>
            </View>
            <SwipeableSlider
              value={casSharpness}
              minimumValue={0}
              maximumValue={100}
              step={5}
              onValueChange={setCasSharpness}
              onSlidingComplete={handleSliderComplete}
              colors={colors}
            />
            <Text variant="bodySmall" style={[styles.sliderValue, { color: colors.primary }]}>
              {casSharpness}%
            </Text>
          </View>
          <Divider />
          <List.Item
            title="Hardware mipmap"
            description="Reduce shimmering on distant textures"
            left={props => <List.Icon {...props} icon="texture" color={colors.primary} />}
            right={() => (
              <Switch
                value={hwMipmap}
                onValueChange={(value) => handleSwitchChange(setHwMipmap, value)}
                color={colors.primary}
              />
            )}
            style={styles.listItem}
          />
          <Divider />
          <List.Item
            title="VSync"
            description="Sync frames to display refresh"
            left={props => <List.Icon {...props} icon="sync" color={colors.primary} />}
            right={() => (
              <Switch
                value={vsync}
                onValueChange={(value) => handleSwitchChange(setVsync, value)}
                color={colors.primary}
              />
            )}
            style={styles.listItem}
          />
        </SectionCard>

        <SectionCard title="Controller" description="Input feedback" icon="gamepad-variant">
          <List.Item
            title="Vibration"
            description="Haptics on supported controllers"
            left={props => <List.Icon {...props} icon="vibrate" color={colors.primary} />}
            right={() => (
              <Switch
                value={vibration}
                onValueChange={(value) => handleSwitchChange(setVibration, value)}
                color={colors.primary}
              />
            )}
            style={styles.listItem}
          />
        </SectionCard>

        <SectionCard title="Performance" description="Runtime profile" icon="rocket-launch-outline">
          <View style={styles.segmentRow}>
            <View style={styles.sliderHeader}>
              <MaterialCommunityIcons name="cpu-64-bit" size={20} color={colors.primary} />
              <Text variant="titleSmall" style={{ color: colors.onSurface, marginLeft: 8 }}>
                CPU core
              </Text>
            </View>
            <SegmentedButtons
              value={cpuCore}
              onValueChange={setCpuCore}
              buttons={[
                { value: 'Dynarec', label: 'Dynarec' },
                { value: 'Interpreter', label: 'Interpreter' },
                { value: 'Cached', label: 'Cached' },
              ]}
              style={styles.segmented}
            />
          </View>
          <Divider />
          <List.Item
            title="Diagnostics overlay"
            description="Show perf HUD when needed"
            left={props => <List.Icon {...props} icon="chart-line" color={colors.primary} />}
            right={() => <Switch value={false} onValueChange={() => {}} disabled />}
            style={styles.listItem}
          />
        </SectionCard>

        <View style={[styles.surfaceHint, surfaceStyle]}>
          <Text variant="labelLarge" style={{ color: colors.onSurface }}>
            Heads up
          </Text>
          <Text variant="bodySmall" style={{ color: colors.onSurfaceVariant, marginTop: 6 }}>
            Buttons are intentionally unwired. The native module will own the actual emulator settings and bridge them
            into React Native in the next step.
          </Text>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: 16, paddingBottom: 48 },
  card: {
    marginBottom: 20,
    borderRadius: 20,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 8,
  },
  cardHeaderLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  cardIcon: {
    marginRight: 12,
  },
  cardTitle: {
    fontWeight: '700',
    fontSize: 18,
  },
  cardDescription: {
    marginTop: 2,
  },
  cardContent: {
    paddingHorizontal: 16,
    paddingBottom: 16,
  },
  listItem: {
    paddingVertical: 8,
  },
  sliderBlock: { 
    paddingVertical: 16,
  },
  sliderHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  sliderValue: {
    textAlign: 'center',
    marginTop: 8,
    fontWeight: '600',
  },
  segmentRow: { 
    paddingVertical: 16,
  },
  segmented: { 
    marginTop: 8,
  },
  surfaceHint: {
    borderWidth: 1,
    borderRadius: 16,
    padding: 20,
    marginBottom: 24,
  },
});

export default SettingsScreen;
